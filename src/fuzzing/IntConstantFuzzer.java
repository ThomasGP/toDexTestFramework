/*
 * Copyright 2013 Thomas Pilot
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fuzzing;

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NegExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.TableSwitchStmt;

public class IntConstantFuzzer extends AbstractFuzzer {
	
	public IntConstantFuzzer() {
		super("IntConstant fuzzer");
	}
	
	private boolean shouldBeReplaced(Value v) {
		if (v instanceof IntConstant) {
			IntConstant constant = (IntConstant) v;
			return constant.value != 0;
		}
		return false;
	}

	@Override
	public void caseAssignStmt(AssignStmt stmt) {
		Value rhs = stmt.getRightOp();
		if (shouldBeReplaced(rhs)) {
			stmt.setRightOp(IntConstant.v(0));
			hasTransformedOnce = true;
			return;
		}
		if (rhs instanceof NegExpr) {
			NegExpr negExpr = (NegExpr) rhs;
			if (shouldBeReplaced(negExpr.getOp())) {
				negExpr.setOp(IntConstant.v(0));
				hasTransformedOnce = true;
				return;
			}
		}
		if (rhs instanceof BinopExpr) {
			replaceLeftOrRight((BinopExpr) rhs);
			return;
		}
		if (rhs instanceof InvokeExpr) {
			replaceFirstArg((InvokeExpr) rhs);
		}
	}

	@Override
	public void caseIfStmt(IfStmt stmt) {
		replaceLeftOrRight((BinopExpr) stmt.getCondition());
	}

	private void replaceLeftOrRight(BinopExpr binOp) {
		if (shouldBeReplaced(binOp.getOp1())) {
			binOp.setOp1(IntConstant.v(0));
			hasTransformedOnce = true;
			return;
		}
		if (shouldBeReplaced(binOp.getOp2())) {
			binOp.setOp2(IntConstant.v(0));
			hasTransformedOnce = true;
			return;
		}
	}
	
	@Override
	public void caseInvokeStmt(InvokeStmt stmt) {
		replaceFirstArg(stmt.getInvokeExpr());
	}

	private void replaceFirstArg(InvokeExpr invokeExpr) {
		for (int argIdx = 0; argIdx < invokeExpr.getArgCount(); argIdx++) {
			Value arg = invokeExpr.getArg(argIdx);
			if (shouldBeReplaced(arg)) {
				invokeExpr.setArg(argIdx, IntConstant.v(0));
				hasTransformedOnce = true;
				return;
			}
		}
	}

	@Override
	public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
		if (shouldBeReplaced(stmt.getKey())) {
			stmt.setKey(IntConstant.v(0));
			hasTransformedOnce = true;
			return;
		}
		// although the lookup values are primitive ints, we see them as potentially replaceable IntConstants
		for (int lookupIdx = 0; lookupIdx < stmt.getTargetCount(); lookupIdx++) {
			int lookupValue = stmt.getLookupValue(lookupIdx);
			if (lookupValue != 0) {
				stmt.setLookupValue(lookupIdx, 0);
				hasTransformedOnce = true;
				return;
			}
		}
	}

	@Override
	public void caseReturnStmt(ReturnStmt stmt) {
		Value returnValue = stmt.getOp();
		if (shouldBeReplaced(returnValue)) {
			stmt.setOp(IntConstant.v(0));
			hasTransformedOnce = true;
			return;
		}
	}
	
	@Override
	public void caseTableSwitchStmt(TableSwitchStmt stmt) {
		if (shouldBeReplaced(stmt.getKey())) {
			stmt.setKey(IntConstant.v(0));
			hasTransformedOnce = true;
			return;
		}
		// although the low/high values are primitive ints, we see them as potentially replaceable IntConstants
		if (stmt.getLowIndex() != 0) {
			stmt.setLowIndex(0);
			hasTransformedOnce = true;
			return;
		}
		if (stmt.getHighIndex() != 0) {
			stmt.setHighIndex(0);
			hasTransformedOnce = true;
			return;
		}
	}

	/*
	 * NOTE for caseThrowStmt(ThrowStmt): the exception thrown is an Immediate,
	 * but replacing it with an IntConstant (which is an Immediate) there is invalid
	 * Jimple anyway. So we do nothing for ThrowStmt.
	 */
}