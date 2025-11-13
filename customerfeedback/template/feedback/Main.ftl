<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.CustomerFeedbackTab}</h3>
  </div>
  <div class="screenlet-body">
    <#if feedbackList?has_content>
      <table class="basic-table" cellspacing="0">
        <thead>
          <tr class="header-row">
            <th>${uiLabelMap.CustomerFeedbackTableCustomer}</th>
            <th>${uiLabelMap.CustomerFeedbackTableRating}</th>
            <th>${uiLabelMap.CustomerFeedbackTableComments}</th>
            <th>${uiLabelMap.CustomerFeedbackTableCreated}</th>
          </tr>
        </thead>
        <tbody>
          <#list feedbackList as feedback>
            <tr class="${feedback_index % 2 == 0?then('even','odd')}">
              <td>${feedback.customerId!}</td>
              <td>${feedback.rating?string["0.##"]!}</td>
              <td>${feedback.comments!}</td>
              <td><#if feedback.createdDate??>${feedback.createdDate?string["yyyy-MM-dd HH:mm"]}</#if></td>
            </tr>
          </#list>
        </tbody>
      </table>
    <#else>
      <p>${uiLabelMap.CustomerFeedbackEmpty}</p>
    </#if>
  </div>
</div>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.CommonMoreInfo}</h3>
  </div>
  <div class="screenlet-body">
    <p>${uiLabelMap.CustomerFeedbackWebappInfo?default("Use the product catalog to review or add customer feedback entries.")}</p>
  </div>
</div>
