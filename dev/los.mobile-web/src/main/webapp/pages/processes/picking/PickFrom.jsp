<%-- 
  Copyright (c) 2010 LinogistiX GmbH

  www.linogistix.com
  
  Project: myWMS-LOS
--%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%--
The taglib directive below imports the JSTL library. If you uncomment it,
you must also add the JSTL library to the project. The Add Library... action
on Libraries node in Projects view can be used to add the JSTL 1.1 library.
--%>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
   		<meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>LOS</title>
        <link rel="stylesheet" href="<%=request.getContextPath()%>/pages/stylesheet.css" type="text/css" />
    </head>
    
    <body class="verticalscroll" topmargin="0" leftmargin="0" marginwidth="0" marginheight="0" onload="load()">
        <f:view locale="#{PickingBean.locale}">
            <f:loadBundle var="bundle" basename ="de.linogistix.mobile.processes.picking.PickingMobileBundle" /> 
            
            <h:form id="Form" styleClass="form" >
                <%-- Page Title--%>
                <p id="pHeader"class="pageheader">
                	<h:outputText id="pagetitle" value="#{bundle.TitleEnterSource}" styleClass="pagetitle"/>
	                <h:graphicImage id="logo" url="/pics/logo.gif" styleClass="logo"/>
               	</p>
                
                <div class="space">
                    <h:messages id="messages"  styleClass="error"/> 
                    
                    <table  width="100%" border="0" cellspacing="0">
                    	<colgroup>
							<col width="10%"/>
							<col width="90%"/>
						</colgroup>
                    	
                    	<tr>
                            <td nowrap="nowrap" style="padding-right:20px">
                            	<h:outputLabel id="orderLabel" value="#{bundle.LabelOrder}:" styleClass="param"/> 
                            </td>
                            <td nowrap="nowrap">
                              	<h:outputLabel id="orderData" value="#{PickingBean.orderInfo}" styleClass="label"/>
                            </td>
                        </tr>
                    	<tr>
                            <td nowrap="nowrap" style="padding-right:20px">
                            	<h:outputLabel id="locLabel" value="#{bundle.LabelLocation}:" styleClass="param"/> 
                            </td>
                            <td nowrap="nowrap">
                              	<h:outputLabel id="locData" value="#{PickingBean.pickFromName}" styleClass="labelBold"/>
                            </td>
                        </tr>
                    	<tr>
                            <td nowrap="nowrap" style="padding-right:20px">
                            	<h:outputLabel id="ulLabel" value="#{bundle.LabelUnitLaod}:" styleClass="param" /> 
                            </td>
                            <td nowrap="nowrap">
                              	<h:outputLabel id="ulData" value="#{PickingBean.pickFromLabel}" styleClass="labelBold" />
                            </td>
                        </tr>
                    	<tr>
                            <td nowrap="nowrap" style="padding-right:20px">
                            	<h:outputLabel id="matLabel" value="#{bundle.LabelItemData}:" styleClass="param"/> 
                            </td>
                            <td nowrap="nowrap">
                              	<h:outputLabel id="matData" value="#{PickingBean.pickItemNumber}" styleClass="label"/>
                            </td>
                        </tr>
                        <tr>
                            <td nowrap="nowrap" colspan="2" style="padding-right:20px; padding-left:20px; font-size:smaller;">
                               	<h:outputLabel id="matName" value="#{PickingBean.pickItemName}" />
                            </td>
                        </tr>
                    </table>
                </div>
                    
                <div class="space">
                    <table  width="100%" border="0" cellspacing="0">
                        <tr><td>&#160;</td></tr>

                        <tr>
                            <td>
                            	<h:outputLabel id="input1Label" value="#{bundle.MsgPickFrom}" styleClass="label" />
                            </td>
                        </tr>
                        <tr>
                            <td width="100%" style="padding-right:4px;" >
                               	<h:inputText id="input1" 
                             			 value="#{PickingBean.inputCode}" 
                             			 styleClass="input" /> 
                            </td>
                        </tr>

                    </table>
                    
					<h:inputText value="IE-Dummy" style="display:none" />
                </div>
                

                <%-- Command Buttons --%>
                <div class="buttonbar">  
                    <h:commandButton id="BUTTON_CONTINUE"
                    	 	value="#{bundle.ButtonContinue}" 
              				 action="#{PickingBean.processPickFrom}" 
              				 styleClass="commandButton"  />
					&#160;
                    <h:commandButton id="BUTTON_SPECIAL" 
	                				 value="#{bundle.ButtonChange}" 
	                				 action="#{PickingBean.processPickFromFunction}" 
	                				 styleClass="commandButton"  />
                </div>
            </h:form>
        </f:view> 
        <script type="text/javascript">
            
            function load() {            
                setFocus();    
            }    
            
            function setFocus() {
                document.getElementById('Form:input1').focus();
                document.getElementById('Form:input1').select();
            }    
            
        </script>
        
    </body>
</html>
