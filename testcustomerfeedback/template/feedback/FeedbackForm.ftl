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
    <h3>${uiLabelMap.TestCustomerFeedbackFormTitle}</h3>
  </div>
  <div class="screenlet-body">
    <#if errorMessage?has_content>
      <div class="errorMessage">${errorMessage}</div>
    </#if>
    <form method="post" action="${request.getContextPath()}/control/feedback/add">
      <input type="hidden" name="externalLoginKey" value="${requestAttributes.externalLoginKey!}"/>
      <div class="field">
        <label for="productId">${uiLabelMap.CommonProduct}</label>
        <input type="text" class="large" name="productId" id="productId" value="${parameters.productId!}" required="required"/>
      </div>
      <div class="field">
        <label for="partyId">${uiLabelMap.CommonParty}</label>
        <input type="text" class="large" name="partyId" id="partyId" value="${parameters.partyId!userLogin.partyId!}"/>
        <p class="help-text">${uiLabelMap.TestCustomerFeedbackPartyHint}</p>
      </div>
      <div class="field">
        <label for="rating">${uiLabelMap.TestCustomerFeedbackRatingLabel}</label>
        <input type="number" step="0.5" min="0" max="5" name="rating" id="rating" value="${parameters.rating!}" required="required"/>
      </div>
      <div class="field">
        <label for="comments">${uiLabelMap.CommonComments}</label>
        <textarea name="comments" id="comments" rows="5" cols="40">${parameters.comments!}</textarea>
      </div>
      <div class="field submit">
        <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonSubmit}"/>
      </div>
    </form>
  </div>
</div>
