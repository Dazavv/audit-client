package com.dazavv.audit.auditclient;

import com.dazavv.audit.auditclient.annotation.Audit;

public class AuditTargetService {

    @Audit(action = "TEST_ACTION", logArgs = false)
    public String successfulOperation(String input) {
        return "result-" + input;
    }

    @Audit(action = "FAILING_ACTION")
    public void failingOperation() {
        throw new RuntimeException("Simulated failure");
    }

    @Audit(action = "ARGS_ACTION", logArgs = true)
    public void operationWithArgs(String sensitiveData) {
    }
}
