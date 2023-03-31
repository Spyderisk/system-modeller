//Utility functions for Threats

export function getThreatStatus(threat, controlStrategies) {
    let threatUri = threat.uri;
    let status = "UNMANAGED";
    let triggeredThreat = false; //can threat be triggered
    let triggered = false; //is threat currently triggered?

    Object.keys(threat.controlStrategies).some((csgKey) => {
        let csg = controlStrategies[csgKey];
        let csgType = threat.controlStrategies[csgKey];
        if (csg.enabled && csgType === "MITIGATE") {
            status = "MITIGATED";
            return true;
        }
    });
    Object.keys(threat.controlStrategies).some((csgKey) => {
        let csg = controlStrategies[csgKey];
        let csgType = threat.controlStrategies[csgKey];
        if (csg.enabled && csgType === "BLOCK") {
            status = "BLOCKED";
            return true;
        }
    });
    Object.keys(threat.controlStrategies).some((csgKey) => {
        let csg = controlStrategies[csgKey];
        let csgType = threat.controlStrategies[csgKey];

        if (csgType === "TRIGGER") {
            triggeredThreat = true;
            if (csg.enabled) {
                triggered = true;
                return true;
            }
        }
    });

    //if still unmanaged, check for acceptance
    if (status === "UNMANAGED" && (threat.acceptanceJustification !== null)) {
        status = "ACCEPTED";
    }
    
    if (triggeredThreat) {
        status += triggered ? "/TRIGGERED" : "/UNTRIGGERED";
    }

    return status;            
}

// This function sets the "triggerable" and "triggered" flags on a threat
// derived from the current settings of the threat CSGs.
// CSGs of type "TRIGGER" make the threat "triggerable"
// If any of these CSGs are enabled, the threat is "triggered"
export function setThreatTriggeredStatus(threat, controlStrategies) {
    let triggerable = false; //can threat be triggered
    let triggered = false; //is threat currently triggered?
    
    Object.keys(threat.controlStrategies).some((csgKey) => {
        let csg = controlStrategies[csgKey];
        let csgType = threat.controlStrategies[csgKey];

        if (csgType === "TRIGGER") {
            triggerable = true;
            if (csg.enabled) {
                triggered = true;
                return true;
            }
        }
       
    });

    threat.triggerable = triggerable;
    threat.triggered = triggered;
}

    
