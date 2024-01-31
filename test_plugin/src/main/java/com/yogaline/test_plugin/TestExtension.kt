package com.yogaline.test_plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TestExtension(
    private val logger: FileLogger,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.accept(TestVisitor(logger, pluginContext), null)
    }
}

internal class TestVisitor(
    private val logger: FileLogger,
    private val pluginContext: IrPluginContext,
) : IrElementVisitor<Unit, Nothing?> {

    private val measureTimeFunSymbol = pluginContext.referenceFunctions(
        callableId = CallableId(
            packageName = FqName("com.example.testapplication"),
            callableName = Name.identifier("measureTime"),
        ),
    ).single {
        it.owner.returnType == pluginContext.irBuiltIns.unitType &&
            it.owner.isInline &&
            it.owner.valueParameters.single().type == pluginContext.irBuiltIns.functionN(0).typeWith(pluginContext.irBuiltIns.unitType)
    }

    override fun visitElement(element: IrElement, data: Nothing?) {
        element.acceptChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: Nothing?) {
        logger.logMsg(
            "visitFunction: ${declaration.name}. Symbol: ${declaration.symbol} $.\nDump: ${declaration.dump()}",
        )

        if (
            declaration.hasAnnotation(FqName("androidx.compose.runtime.Composable")).not() ||
            declaration.symbol == measureTimeFunSymbol ||
            declaration.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        ) {
            return super.visitFunction(declaration, data)
        }

        val builder = DeclarationIrBuilder(pluginContext, declaration.symbol)
        val measureTimeCall = builder.irCall(measureTimeFunSymbol)

        val lambdaFunction = pluginContext.irFactory.buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = Name.special("<anonymous>")
            returnType = pluginContext.irBuiltIns.unitType
            visibility = DescriptorVisibilities.LOCAL
            modality = Modality.FINAL
        }

        lambdaFunction.body = builder.irBlockBody {
            declaration.body?.statements?.forEach {
                +it
            }
        }

        val functionExpression = IrFunctionExpressionImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = pluginContext.irBuiltIns.functionN(0).typeWith(pluginContext.irBuiltIns.unitType),
            function = lambdaFunction,
            origin = IrStatementOrigin.LAMBDA,
        )

        measureTimeCall.putValueArgument(0, functionExpression)

        declaration.body = builder.irBlockBody {
            +measureTimeCall
        }.also {
            it.patchDeclarationParents(declaration)
        }

        logger.logMsg("Dump after modification:\n${declaration.dump()}")
    }

    override fun visitCall(expression: IrCall, data: Nothing?) {
        logger.logMsg("visitCall: ${expression.symbol.owner.name}, argument: \n${expression.valueArguments.firstOrNull()?.dump()}")
        super.visitCall(expression, data)
    }
}
