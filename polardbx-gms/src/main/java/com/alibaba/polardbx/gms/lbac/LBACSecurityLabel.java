/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.polardbx.gms.lbac;

import com.alibaba.polardbx.gms.lbac.component.ComponentInstance;

/**
 * @author pangzhaoxing
 */
public class LBACSecurityLabel {

    private String labelName;

    private String policyName;

    private ComponentInstance[] components;

    public LBACSecurityLabel(ComponentInstance[] components) {
        this.components = components;
    }

    public LBACSecurityLabel(String labelName, String policyName, ComponentInstance[] components) {
        this.labelName = labelName;
        this.policyName = policyName;
        this.components = components;
    }

    public ComponentInstance[] getComponents() {
        return components;
    }

    public void setComponents(ComponentInstance[] components) {
        this.components = components;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }
}
