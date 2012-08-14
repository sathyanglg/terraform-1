/*******************************************************************************
 * Copyright 2012 Urbancode, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.urbancode.terraform.tasks.aws;

import org.apache.log4j.Logger;
import org.urbancode.terraform.tasks.common.Context;


public class VpcSecurityGroupRefTask extends SecurityGroupRefTask {

    //**********************************************************************************************
    // CLASS
    //**********************************************************************************************
    final static private Logger log = Logger.getLogger(SecurityGroupRefTask.class);

    //**********************************************************************************************
    // INSTANCE
    //**********************************************************************************************

    //----------------------------------------------------------------------------------------------
    public VpcSecurityGroupRefTask(Context context) {
        super(context);
    }

    /**
     *
     * @return the group
     * @throws Exception
     */
    @Override
    public SecurityGroupTask fetchSecurityGroup() {
        if (ref == null) {
            if (context.getEnvironment() instanceof EnvironmentTaskAWS) {
                EnvironmentTaskAWS env = (EnvironmentTaskAWS) context.getEnvironment();

                if (env.getVpc() != null) {
                    log.debug("Looking for Security Group " + groupName);

                    ref = env.getVpc().findSecurityGroupForName(groupName);
                }
            }
            else {
                // not supported atm
            }
        }

        return ref;
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void create() {

    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void destroy() {
        ref = null;
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void restore() {
        // TODO Auto-generated method stub

    }
}