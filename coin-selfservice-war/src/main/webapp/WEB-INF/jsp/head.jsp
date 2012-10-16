<!doctype html>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="include.jsp" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>
    <spring:message code="jsp.general.pageTitle" arguments="${param.title}"/>
  </title>

  <c:choose>
    <c:when test="${dev eq true}">
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-2.0.4.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-alert.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-button.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-datepicker.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-dropdown.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-form.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-generic.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-modal.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-navbar.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-pagination.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-popover.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-responsive.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-table.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/bootstrap-tooltip.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/component-datatables.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/font-awesome.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/generic.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/layout.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/screen.css"/>"/>
      <link rel="stylesheet" href="<c:url value="/css/select2.css"/>"/>
    </c:when>
    <c:otherwise>
      <link rel="stylesheet" href="<c:url value="/css/style.min.css"/>"/>
    </c:otherwise>
  </c:choose>

  <!--[if lt IE 9]>
  <script src="<c:url value="/js/tools/html5shiv.js"/>"></script>
  <![endif]-->
</head>
<body>
<script>document.body.className = 'js-loading'</script>
<spring:url value="/app-overview.shtml" var="homeUrl" htmlEscape="true"/>

<header class="header">
  <a class="logo" href="${homeUrl}">
    <img src="<c:url value="/images/surf-conext-logo.png"/>" alt="Surf Conext">
  </a>

  <sec:authorize access="hasAnyRole('ROLE_USER','ROLE_ADMIN')">
    <nav class="primary-navigation">
        <ul>
          <li class="user"><spring:message code="jsp.general.welcome"/> <a href="index.html"><sec:authentication property="principal.displayName" scope="request" htmlEscape="true"/></a></li>

          <sec:authorize access="hasRole('ROLE_ADMIN')" var="isAdmin"/>
            <c:set var="userclass">
              <c:choose>
                <c:when test="${currentrole eq 'ROLE_USER'}">user-role-user</c:when>
                <c:when test="${currentrole eq 'ROLE_ADMIN'}">user-role-manager</c:when>
              </c:choose>
            </c:set>
            <li class="role-switch">
              <ul class="user-dropdown">
                <li class="active">
                  <a href="${homeUrl}">
                    <tags:providername provider="${selectedidp}"/>
                  </a>
                </li>



                <c:if test="${isAdmin eq true}">
                  <sec:authentication property="principal.idp" var="ownIdp" scope="request"/>
                    <c:forEach items="${idps}" var="idp">
                      <c:choose>
                        <c:when test="${currentrole eq 'ROLE_ADMIN' and idp.id eq ownIdp}">
                          <c:set var="userclass">user-role-user</c:set>
                          <c:set var="newrole">ROLE_USER</c:set>
                        </c:when>
                        <c:otherwise>
                          <c:set var="userclass">user-role-manager</c:set>
                          <c:set var="newrole">ROLE_ADMIN</c:set>
                        </c:otherwise>
                      </c:choose>
                      <c:if test="${newrole eq 'ROLE_USER' and selectedidp.id ne ownIdp}">
                        <%-- Corner case: admin can control multiple IdPs of his institution and controls currently a different
                            IdP than his home organisation. --%>
                        <li class="user-role-manager" data-roleId="${idp.id}">
                          <spring:url var="toggleLink" value="/app-overview.shtml" htmlEscape="true">
                            <spring:param name="idpId" value="${idp.id}"/>
                            <spring:param name="role" value="ROLE_ADMIN"/>
                          </spring:url>
                            <a href="${toggleLink}">
                              <tags:providername provider="${idp}"/>
                            </a>
                        </li>
                      </c:if>
                      <li class="${userclass}" data-roleId="${idp.id}">
                        <spring:url var="toggleLink" value="/app-overview.shtml" htmlEscape="true">
                          <spring:param name="idpId" value="${idp.id}"/>
                          <spring:param name="role" value="${newrole}"/>
                        </spring:url>
                          <a href="${toggleLink}">
                            <tags:providername provider="${idp}"/>
                          </a>
                      </li>
                    </c:forEach>
                </c:if>



              </ul>
            </li>

        <li class="logout"><a href="<spring:url value="/j_spring_security_logout" htmlEscape="true" />"><spring:message code="jsp.general.logout"/></a></li>
      </ul>
  </nav>

  </sec:authorize>
</header>




<c:choose>
  <c:when test="${not empty param.wrapperAdditionalCssClass}">
    <div class="wrapper ${param.wrapperAdditionalCssClass}">
  </c:when>
  <c:otherwise>
    <div class="wrapper has-left">
  </c:otherwise>
</c:choose>

  <div class="column-left menu-holder">
    <nav class="secondary-menu">
      <ul>
        <c:forEach items="${menu.menuItems}" var="menuItem">
          <li<c:if test="${menuItem.selected}"> class="active"</c:if>>
            <spring:url value="${menuItem.url}" htmlEscape="true" var="url"/>
            <a href="${url}"><spring:message code="${menuItem.label}"/></a>
          </li>
        </c:forEach>
      </ul>
    </nav>
  </div>