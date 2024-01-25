package com.craftinginterpreters.lox;

import java.util.List;

import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Unary;
import com.craftinginterpreters.lox.Stmt.Expression;
import com.craftinginterpreters.lox.Stmt.Print;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	@Override
	public Object visitBinaryExpr(Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
			case GREATER:
				checkNumberOperands(expr.operator, left, right);
				return (double) left > (double) right;
			case GREATER_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left >= (double) right;
			case LESS:
				checkNumberOperands(expr.operator, left, right);
				return (double) left < (double) right;
			case LESS_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left <= (double) right;
			case MINUS:
				checkNumberOperands(expr.operator, left, right);
				return (double) left - (double) right;
			case SLASH:
				checkNumberOperands(expr.operator, left, right);
				if ((Double) right == 0.0) {
					throw new RuntimeError(expr.operator, "Division by zero.");
				}
				return (double) left / (double) right;
			case STAR:
				/* Python inspired string multiplication */
				if (left instanceof String && right instanceof Double) {
					return stringMultiplication((String) left, ((Double) right).intValue());
				}
				if (left instanceof Double && right instanceof String) {
					return stringMultiplication((String) right, ((Double) left).intValue());
				}
				/* End string multiplication */
				checkNumberOperands(expr.operator, left, right);
				return (double) left * (double) right;
			case MODULO:
				checkNumberOperands(expr.operator, left, right);
				return (double) left % (double) right;
			case PLUS:
				if (left instanceof Double && right instanceof Double) {
					return (double) left + (double) right;
				}
				if (left instanceof String && right instanceof String) {
					return (String) left + (String) right;
				}
				/* Allow plus for string and double */
				if (left instanceof String && right instanceof Double) {
					String num = ((Double) right).toString();
					if (num.endsWith(".0")) {
						num = num.substring(0, num.length() - 2);
					}
					return (String) left + num;
				}
				if (left instanceof Double && right instanceof String) {
					String num = ((Double) left).toString();
					if (num.endsWith(".0")) {
						num = num.substring(0, num.length() - 2);
					}
					return num + (String) right;
				}
				/* End allow plus for string and double */
				throw new RuntimeError(expr.operator, "Operands must be numbers or strings.");
			default:
				break;
		}

		// Unreachable
		return null;
	}

	@Override
	public Object visitGroupingExpr(Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitLiteralExpr(Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitUnaryExpr(Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case MINUS:
				checkNumberOperand(expr.operator, right);
				return -(double) right;
			case BANG:
				return !isTruthy(right);
			default:
				break;
		}

		// Unreachable
		return null;
	}

	private void checkNumberOperand(Token operator, Object right) {
		if (right instanceof Double)
			return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double)
			return;
		throw new RuntimeError(operator, "Operands must be numbers.");
	}

	private boolean isTruthy(Object object) {
		if (object == null)
			return false;
		if (object instanceof Boolean)
			return (boolean) object;
		return true;
	}

	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null)
			return false;
		return a.equals(b);
	}

	private String stringify(Object object) {
		if (object == null)
			return "nil";
		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}
		return object.toString();
	}

	private String stringMultiplication(String str, int amount) {
		amount = amount < 1 ? 0 : amount;
		StringBuilder sb = new StringBuilder();
		sb.append(str);
		while (amount > 1) {
			sb.append(str);
			amount--;
		}
		return sb.toString();
	}

	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	@Override
	public Void visitExpressionStmt(Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}

	@Override
	public Void visitPrintStmt(Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}
}
