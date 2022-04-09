package com.bnorm.template

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.jvm.ir.hasChild
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName

class BodySwapper(
  private val pluginContext: IrPluginContext,
  private val firstAnnotationClass: IrClassSymbol,
  private val secondAnnotationClass: IrClassSymbol,
) : IrElementTransformerVoidWithContext() {
  private val typeUnit = pluginContext.irBuiltIns.unitType
  private var firstFun: IrFunction? = null
  private var secondFun: IrFunction? = null
  private var secondBody: IrBody? = null
  private var secondVars:  List<IrValueParameter>? = null
  private var secondRet: IrType? = null
  private var done = false

  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    val tempBody = declaration.body
    if (tempBody != null) {
      when {
        declaration.hasAnnotation(firstAnnotationClass) -> {
          //println(tempBody.statements[0])
          done = false
          firstFun = declaration
        }
        declaration.hasAnnotation(secondAnnotationClass) -> {
          println(tempBody.statements)
          secondBody = tempBody
          if (declaration.valueParameters.isNotEmpty())
          secondVars = declaration.valueParameters
          secondRet = declaration.returnType
          secondFun = declaration
        }
      }
    }
    if (!done && firstFun!=null && secondFun!=null){
      secondFun!!.valueParameters = firstFun!!.valueParameters
      firstFun!!.valueParameters = secondVars as List<IrValueParameter>

      for (i in firstFun!!.valueParameters)
        i.parent = firstFun as IrFunction
      for (i in secondFun!!.valueParameters)
        i.parent = secondFun as IrFunction

      if (firstFun!!.returnType != typeUnit || secondFun!!.returnType != typeUnit){
        secondFun!!.body = irSwapper(secondFun!!, firstFun!!.body!!)
        firstFun!!.body = irSwapper(firstFun!!, secondBody!!)
      } else {
        secondFun!!.body = firstFun!!.body
        firstFun!!.body = secondBody
      }

       //for (i in firstFun!!.body!!.statements)
          //i.transform(IrReturnImplTransformer(firstFun!!), null)
       //for (i in secondFun!!.body!!.statements)
          //i.transform(IrReturnImplTransformer(secondFun!!), null)

      done = true
      secondFun = null
      firstFun = null
      secondBody = null
      secondVars = null
    }
    return super.visitFunctionNew(declaration)
  }

  private fun irSwapper(
    function: IrFunction,
    body: IrBody
  ):IrBlockBody {
    return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
      val tryBlock = irBlock(resultType = function.returnType) {
        for (statement in body.statements) {
          val expression = statement as IrReturnImpl
          if (expression.returnTargetSymbol != function.symbol)
            +DeclarationIrBuilder(
              pluginContext, function.symbol,
              expression.startOffset, expression.endOffset
            ).irReturn(expression.value)
          else +statement
        }
      }
        +IrTryImpl(startOffset, endOffset, tryBlock.type).also { irTry ->
          irTry.tryResult = tryBlock
        }
    }
  }

  inner class IrReturnImplTransformer(
    private val function: IrFunction
  ) : IrElementTransformerVoidWithContext() {
    override fun visitReturn(expression: IrReturn): IrExpression {
      if (expression.returnTargetSymbol == function.symbol) return super.visitReturn(expression)
      val temporary = DeclarationIrBuilder(pluginContext,function.symbol,
        expression.startOffset,expression.endOffset).irReturn(expression.value)

      return DeclarationIrBuilder(pluginContext, function.symbol,
        expression.startOffset, expression.endOffset).irBlock {
        val result = irTemporary(temporary.value)
        +temporary.apply {
          value = irGet(result)
        }
      }
      }
  }

  private fun irBodyMaker(function: IrFunction): IrBlockBody =
    DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody{}
}
