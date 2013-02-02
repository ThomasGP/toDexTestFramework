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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import soot.Body;
import soot.BodyTransformer;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.BreakpointStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NopStmt;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.StmtSwitch;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;

// Tranformer that transforms at most one statement in one body of soot's input
public abstract class AbstractFuzzer extends BodyTransformer implements StmtSwitch {
	
	protected boolean hasTransformedOnce = false;
	
	protected final String name;
	
	protected final Logger LOG;
	
	public AbstractFuzzer(String name) {
		this.name = name;
		this.LOG = LogManager.getLogger(name);
	}
	
	@Override
	protected void internalTransform(Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
		if (hasTransformedOnce) {
			return;
		}
		for (Unit u : b.getUnits()) {
			if (u instanceof Stmt) {
				u.apply(this);
				if (hasTransformedOnce) {
					LOG.debug("{} transformed statement {} in method {}", name, b.getMethod(), u);
					break;
				}
			}
		}
	}
	
	@Override
	public void caseBreakpointStmt(BreakpointStmt stmt) {
	}

	@Override
	public void caseInvokeStmt(InvokeStmt stmt) {
	}

	@Override
	public void caseAssignStmt(AssignStmt stmt) {
	}

	@Override
	public void caseIdentityStmt(IdentityStmt stmt) {
	}

	@Override
	public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
	}

	@Override
	public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
	}

	@Override
	public void caseGotoStmt(GotoStmt stmt) {
	}

	@Override
	public void caseIfStmt(IfStmt stmt) {
	}

	@Override
	public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
	}

	@Override
	public void caseNopStmt(NopStmt stmt) {
	}

	@Override
	public void caseRetStmt(RetStmt stmt) {
	}

	@Override
	public void caseReturnStmt(ReturnStmt stmt) {
	}

	@Override
	public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
	}

	@Override
	public void caseTableSwitchStmt(TableSwitchStmt stmt) {
	}

	@Override
	public void caseThrowStmt(ThrowStmt stmt) {
	}

	@Override
	public void defaultCase(Object obj) {
	}
}