/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2023
//
// Copyright in this software belongs to University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//      Created By:             Panos Melas
//      Created Date:           2023-01-24
//      Created for Project :   Cyberkit4SME
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.modelvalidator.attackpath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Or;
import com.bpodgursky.jbool_expressions.Variable;
import com.bpodgursky.jbool_expressions.options.ExprOptions;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

public class LogicalExpression {
    private static final Logger logger = LoggerFactory.getLogger(AttackNode.class);

    private boolean allRequired;

    private List<Expression<String>> allCauses = new ArrayList<>();
    private Expression<String> cause;

    public LogicalExpression(AttackPathDataset ds, List<Object> cList, boolean ar) {

        this.allRequired = ar;

        List<Expression<String>> allCausesAux = new ArrayList<>();
        for(Object causeObj : cList) {
            if (causeObj instanceof LogicalExpression) {
                LogicalExpression leObj = (LogicalExpression) causeObj;
                allCausesAux.add(leObj.getCause());
            } else {
                Expression exprObj = (Expression) causeObj;
                allCausesAux.add(exprObj);
            }
        }

        //all_causes = [cc for cc in all_causes if cc is not None]
        for (Expression<String> cc : allCausesAux) {
            if(cc != null) {
                allCauses.add(cc);
            }
        }
        
        if (allCauses.size() == 0){
            this.cause = null;
        } else if(allCauses.size() == 1) {
            this.cause = allCauses.get(0);
        } else {
            if (allRequired) {
                Expression ands = And.of(allCauses);
                this.cause = RuleSet.simplify(ands);
            } else {
                Expression ors = Or.of(allCauses);
                this.cause = RuleSet.simplify(ors);
            }
        }
        
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Set<String> uris = this.uris();
        Iterator<String> it = uris.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public void setCause(Expression expr) {
        this.cause = expr;
    }
    public Expression getCause(){
        if(this.cause == null){
            return this.cause;
        } else {
            return RuleSet.simplify(this.cause);
        }
    }

    public void applyDNF(int maxComplexity){
        // apply DNF
        if (this.cause == null){
            return;
        }
        int causeComplexity = this.cause.getChildren().size();
        if (causeComplexity <= maxComplexity){
            this.cause = RuleSet.toDNF(RuleSet.simplify(this.cause));
        }
    }

    public Set<String> uris(){
        Set<String> symbolSetUris = new HashSet<String>();
        if(this.cause != null) {
            for(Expression<String> symbol : this.cause.getChildren()){
                symbolSetUris.add(symbol.toString());
            }
            if(symbolSetUris.isEmpty()){
                symbolSetUris.add(this.cause.toString());
            }
        }
        return symbolSetUris;
    }

    public String getCsgComment(String dummyUri){
        if(!dummyUri.startsWith("system#")){
            dummyUri = "system#" + dummyUri;
        }
        //MyControlStrategy myCSG = new MyControlStrategy("", "", "");
        return "";
    }

}

