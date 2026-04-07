/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.expression;

import java.util.ArrayList;
import java.util.List;

public class BooleanFunction implements IExpression<Boolean>
{
	public static enum LogicalOperator {AND,OR};
	
	private List<IExpression<Boolean>> operandList = new ArrayList<IExpression<Boolean>>();
	private LogicalOperator operator = LogicalOperator.AND;
	private boolean invert = false;
	
	public BooleanFunction()
	{
		super();
	}
	
	public BooleanFunction(LogicalOperator operator)
	{
		super();
		this.operator = operator;
	}
	
	public BooleanFunction(LogicalOperator operator, boolean invert)
	{
		super();
		this.operator = operator;
		this.invert = invert;
	}
	
	public BooleanFunction addOperand(IExpression<Boolean> operand)
	{
		this.operandList.add(operand);
		return this;
	}

	@Override
	public Class<Boolean> getExpressionType()
	{
		return Boolean.class;
	}

	@Override
	public String getExpressionString()
	{
		StringBuilder expressionString = new StringBuilder();
		if(this.invert)
		{
			expressionString.append("!(");
		}
		boolean first = false;
		for(IExpression<Boolean> operand : this.operandList)
		{
			if(first)
			{
				first = false;
				expressionString.append(" " );
			}
			else
			{
				expressionString.append(this.operator == LogicalOperator.OR ? " or " : " and ");
			}
			
			if(operand instanceof BooleanFunction)
			{
				expressionString.append("(" );
			}
			expressionString.append(operand.getExpressionString());
			if(operand instanceof BooleanFunction)
			{
				expressionString.append(")" );
			}
		}
		if(this.invert)
		{
			expressionString.append(")");
		}
		return expressionString.toString();
	}

	@Override
	public Boolean evaluate(Context context)
	{
		if(this.operandList.isEmpty())
		{
			throw new IllegalStateException("operand list is empty");
		}
		boolean currentResult = false;

		if(this.operator == LogicalOperator.AND)
		{
			currentResult = true;
			for(IExpression<Boolean> expression : this.operandList)
			{
				if(! Boolean.TRUE.equals(expression.evaluate(null)))
				{
					currentResult = false;
					break;
				}
			}
		}
		else
		{
			for(IExpression<Boolean> expression : this.operandList)
			{
				if( Boolean.TRUE.equals(expression.evaluate(null)))
				{
					currentResult = true;
					break;
				}
			}
		}
		return this.invert? ! currentResult : currentResult;
	}

	@Override
	public void dispose()
	{
		IExpression.super.dispose();
		if(this.operandList != null)
		{
			for(IExpression<Boolean> operand : this.operandList)
			{
				operand.dispose();
			}
			this.operandList.clear();
		}
		this.operandList = null;
	}

	
}
