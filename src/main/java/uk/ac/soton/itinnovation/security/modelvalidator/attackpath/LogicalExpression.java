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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Or;
import com.bpodgursky.jbool_expressions.Variable;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

public class LogicalExpression {
    private static final Logger logger = LoggerFactory.getLogger(LogicalExpression.class);

    private static int instanceCount = 0; // Static counter variable

    private boolean allRequired;

    private List<Expression<String>> allCauses = new ArrayList<>();
    private Expression<String> cause;

    public LogicalExpression(AttackPathDataset ds, List<Object> cList, boolean ar) {

        instanceCount++;

        this.allRequired = ar;

        for (Object causeObj : cList) {
            if (causeObj instanceof LogicalExpression) {
                LogicalExpression leObj = (LogicalExpression) causeObj;
                if (leObj.getCause() != null) {
                    //logger.debug("Looks like {} a LE : {}", instanceCount, leObj.getCause().toString().substring(7));
                    allCauses.add(leObj.getCause());
                } else {
                    //logger.debug("Looks like {} a LE : None", instanceCount);
                }
            } else {
                Expression exprObj = (Expression) causeObj;
                if (exprObj != null) {
                    //logger.debug("Looks like {} a string : {} {}", instanceCount, causeObj, causeObj.getClass().getName());
                    //logger.debug("Looks like {} a string : {}", instanceCount, causeObj.toString().substring(7));

                    allCauses.add(exprObj);
                } else {
                    //logger.debug("Looks like {} a string : None", instanceCount);
                }
            }
        }

        if (allCauses.isEmpty()) {
            //logger.debug("ATTENTION LE has a empty cause {}, {}, {}", cList.size(), cList, allCauses.size());
        }

        if (allCauses.size() == 0) {
            this.cause = null;
        } else if (allCauses.size() == 1) {
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
        sb.append("LE{{");
        Set<String> uris = this.uris();
        Iterator<String> it = uris.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}}");
        return sb.toString();
    }

    public void setCause(Expression expr) {
        this.cause = expr;
    }

    public Expression getCause() {
        if (this.cause == null) {
            //logger.debug("Return a null cause");
            return this.cause;
        } else {
            return RuleSet.simplify(this.cause);
        }
    }

    public void applyDNF(int maxComplexity) {
        // apply DNF
        if (this.cause == null) {
            return;
        }
        int causeComplexity = this.cause.getChildren().size();
        if (causeComplexity <= maxComplexity) {
            this.cause = RuleSet.toDNF(RuleSet.simplify(this.cause));
        }
    }

    public Set<String> uris() {
        Set<String> symbolSetUris = new HashSet<String>();
        if (this.cause != null) {
            for (Expression<String> symbol : this.cause.getChildren()) {
                symbolSetUris.add(symbol.toString());
            }
            if (symbolSetUris.isEmpty()) {
                logger.debug("EMPTY URI");
                symbolSetUris.add(this.cause.toString());
            }
        }
        return symbolSetUris;
    }

    public String getCsgComment(String dummyUri) {
        if (!dummyUri.startsWith("system#")) {
            dummyUri = "system#" + dummyUri;
        }
        // MyControlStrategy myCSG = new MyControlStrategy("", "", "");
        return "";
    }
    public List<Expression> getListFromOr() {
        List<Expression> retVal = new ArrayList<>();
        if (this.cause == null) {
            logger.debug("Logical Expression cause is none, cannot find mitigation CSG");
        } else if (this.cause instanceof Or) {
            for (Expression expr : this.cause.getChildren()) {
                retVal.add(expr);
                //logger.debug("convert CSG Or option, adding {} type: {}", expr, expr.getClass().getName());
            }
        } else if (this.cause instanceof And) {
            for (Expression expr : this.cause.getChildren()) {
                retVal.add(expr);
                logger.debug("convert CSG And option, adding {}", expr);
            }
        } else {
            logger.debug("convert_CSG_options: Logical Expression operator not supported");
        }

        return retVal;
    }

    public List<Variable> getListFromAnd(Expression expression) {
        List<Variable> retVal = new ArrayList<>();

        if (expression instanceof And) {
            for (Object obj : expression.getChildren()) {
                if (obj instanceof Variable) {
                    retVal.add((Variable)obj);
                }
            }
        } else if (expression instanceof Variable) {
           retVal.add((Variable)expression);
        } else {
            logger.debug("convert_CSG_options: Logical Expression operator not supported");
        }
        return retVal;
    }

    public List<Expression> convertCSGSymbols(List<Expression> leList) {
        List<Expression> csgUris = new ArrayList<>();
        /*
        for (Expression expr : leList) {
            if (expr instanceof Expression)  {
                csgUris.add(expr);
            } else if (expr instanceof And) {
                for (Expression expr : expr) {
                    csgUris.add(expr);
                }
            } else {
                logger.debug("Logical operator not supported {}", le);
            }
        }
        */
        return csgUris;
    }
}
