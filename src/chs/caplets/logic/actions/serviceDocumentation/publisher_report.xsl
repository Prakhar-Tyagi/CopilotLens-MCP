<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <!--
         * Copyright 2007 Mentor Graphics Corporation. All Rights Reserved.
         * <p>
         * Recipients who obtain this code directly from Mentor Graphics use it solely
         * for internal purposes to serve as example for customising Capital Enterprise Reporter.
         * Recipients may  duplicate the code provided that all notices are fully reproduced with
         * and remain in the code. No part of this code may be modified, reproduced,
         * translated, used, distributed, disclosed or provided to third parties
         * without the prior written consent of Mentor Graphics, except as expressly
         * authorized above.
         * <p>
         * THE CODE IS MADE AVAILABLE "AS IS" WITHOUT WARRANTY OR SUPPORT OF ANY KIND.
         * MENTOR GRAPHICS OFFERS NO EXPRESS OR IMPLIED WARRANTIES AND SPECIFICALLY
         * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
         * OR WARRANTY OF NON-INFRINGEMENT. IN NO EVENT SHALL MENTOR GRAPHICS OR ITS
         * LICENSORS BE LIABLE FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL
         * DAMAGES (INCLUDING LOST PROFITS OR SAVINGS) WHETHER BASED ON CONTRACT, TORT
         * OR ANY OTHER LEGAL THEORY, EVEN IF MENTOR GRAPHICS OR ITS LICENSORS HAVE BEEN
         * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
         * <p>
         -->

    <xsl:param name="urllocation">/reporter/images</xsl:param>
    <xsl:param name="username"></xsl:param>
    <xsl:param name="noResultsToDisplay">No results to Display</xsl:param>
    <xsl:param name="csvmode">false</xsl:param>
    <xsl:param name="quotedval">false</xsl:param>
    <xsl:param name="decimalseperator">default</xsl:param>
    <xsl:param name="shoulddisableoutputescaping">yes</xsl:param>
    <xsl:param name="svglocation">notinitialized</xsl:param>
    <xsl:output method="xhtml" omit-xml-declaration="yes" doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"/>
    <xsl:param name="bgcolor">white</xsl:param>
    <xsl:param name="defaultImageWidth" as="xs:integer">500</xsl:param>
    <xsl:param name="defaultImageHeight" as="xs:integer">410</xsl:param>
    <xsl:strip-space elements="*"/>
    <xsl:decimal-format
            name="de-German"
            decimal-separator=","
            grouping-separator="."/>

    <xsl:decimal-format
            decimal-separator="."
            grouping-separator=","/>
    <xsl:decimal-format
            name="fr-French"
            decimal-separator=","
            grouping-separator=" "/>
    <xsl:template match="report">
        <xsl:param name="tableid">tableid</xsl:param>
        <html>
            <head>

                <title>
                    <xsl:value-of select="@title"/>
                </title>
                <!--pdf converter is not compatible with  css<link href="/reporter/css/default.css" rel="stylesheet" type="text/css" />-->
            </head>
            <body>
                <xsl:call-template name="writeheader"/>
                <xsl:choose>
                    <xsl:when test="not(reportmodule/resultset)">
                        <h2>
                            <xsl:value-of select="$noResultsToDisplay"/>
                        </h2>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="reportmodule">
                            <xsl:with-param name="tableindex" select="$tableid"></xsl:with-param>
                            <xsl:with-param name="resultsetcount">notinitialized</xsl:with-param>
                        </xsl:apply-templates>
                    </xsl:otherwise>
                </xsl:choose>
            </body>
        </html>
    </xsl:template>


    <!-- print a report module as a  html table -->
    <xsl:template match="reportmodule">
        <xsl:param name="tableindex"/>
        <xsl:param name="resultsetcount"/>
        <xsl:param name="addindexcolumn">
            <xsl:value-of select="@addIndexColumn"></xsl:value-of>
        </xsl:param>
        <xsl:param name="groupByColPos">-1</xsl:param>
        <!-- Describe the report body -->
        <table class="reportTableStyle">

            <xsl:attribute name="id">
                <xsl:choose>
                    <xsl:when test="$resultsetcount='notinitialized'">
                        <xsl:value-of select="concat($tableindex, position())"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="concat($tableindex, '-',$resultsetcount)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:variable name="tableid">
                <xsl:choose>
                    <xsl:when test="$resultsetcount='notinitialized'">
                        <xsl:value-of select="position()"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$resultsetcount"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <!-- count number of visibe columns  -->
            <xsl:variable name="headers">
                <xsl:copy-of select="resultsetheader"/>
            </xsl:variable>
            <xsl:variable name="numListInSeperateColumn">
                <xsl:value-of select="count($headers/resultsetheader[(@listInSeperateColumn)])"></xsl:value-of>
            </xsl:variable>
            <xsl:variable name="numcols">
                <xsl:choose>
                    <xsl:when test="$numListInSeperateColumn &gt; 0">
                        <xsl:value-of select="xs:integer($numListInSeperateColumn + 1)"></xsl:value-of>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when
                                    test="count($headers/resultsetheader[not(@spanAllCols) and not(@invisible)]) &gt; 0">
                                <xsl:value-of
                                        select="count($headers/resultsetheader[not(@spanAllCols) and not(@invisible)])"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>1</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <!-- xsl fo friendly node-->
            <colgroup>
                <xsl:attribute name="span">
                    <xsl:value-of select="$numcols"/>
                </xsl:attribute>
            </colgroup>
            <tr>
                <td>
                    <xsl:attribute name="colSpan">
                        <xsl:value-of select="$numcols"/>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="@disableOutputEscaping and (@disableOutputEscaping = 'yes')">
                            <xsl:value-of disable-output-escaping="yes" select="@title"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@title"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>

            <xsl:choose>
                <xsl:when test="not(groupBy)">
                    <xsl:call-template name="header">
                        <xsl:with-param name="colspan" as="xs:integer" select="$numcols"></xsl:with-param>
                        <xsl:with-param name="addindexcolumn"
                                        select="$addindexcolumn"></xsl:with-param>
                    </xsl:call-template>
                    <xsl:variable name="st1" select="sortBy[1]/@title"/>
                    <xsl:variable name="st2" select="sortBy[2]/@title"/>
                    <xsl:variable name="ordering1">
                        <xsl:choose>
                            <xsl:when test="sortBy[1]/@order">
                                <xsl:value-of select="sortBy[1]/@order"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'ascending'"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:variable name="ordering2">ascending</xsl:variable>
                    <!--<xsl:choose>-->
                    <!--<xsl:when test="sortBy[2]/@order ne ''">-->
                    <!--<xsl:value-of select="sortBy[2]/@order"/>-->
                    <!--</xsl:when>-->
                    <!--<xsl:otherwise>-->
                    <!--<xsl:value-of select="ascending"/>-->
                    <!--</xsl:otherwise>-->
                    <!--</xsl:choose>-->
                    <!--</xsl:variable>-->

                    <xsl:variable name="sortByTitlePos1" as="xs:integer">
                        <xsl:choose>
                            <xsl:when test="count( $headers/resultsetheader[@title = $st1 ]) &gt; 0">
                                <xsl:for-each select="$headers/resultsetheader">
                                    <xsl:if test="@title=$st1">
                                        <xsl:value-of select="position()"/>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="1"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:variable name="sortUsingNumbers" >
                        <xsl:choose>
                            <xsl:when
                                    test="$headers/resultsetheader[position() = $sortByTitlePos1 ]/@onlyNumericValues='true'">
                                <xsl:value-of select="'true'"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'false'"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <xsl:variable name="sortByTitlePos2" as="xs:integer">
                        <xsl:choose>
                            <xsl:when test="count( $headers/resultsetheader[@title = $st2]) &gt; 0">
                                <xsl:for-each select="$headers/resultsetheader">
                                    <xsl:if test="@title=$st2">
                                        <xsl:value-of select="position()"/>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="2"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <!--print row (only those have a entry spanning atleast a single row space)-->
                    <xsl:choose>
                        <!-- If the outer report module has group by attribute and the inner report module has accumulateRows custome attribute-->
                        <xsl:when test="$groupByColPos != -1 and @accumulateRows = 'true'">
                            <xsl:choose>
                                <xsl:when test="$sortUsingNumbers='true'">
                                    <xsl:apply-templates select="current-group()/reportmodule[position()]/resultset">

                                        <xsl:sort select="result[position()=$sortByTitlePos1]" order="{$ordering1}"
                                                  data-type="number"/>

                                        <xsl:with-param name="addindexcolumn"
                                                        select="$addindexcolumn"></xsl:with-param>
                                        <xsl:with-param name="numcols" select="$numcols"/>
                                        <xsl:with-param name="headers" select="$headers"/>
                                        <xsl:with-param name="rpos" select="position()"/>
                                        <xsl:with-param name="resultsetcount" select="$tableid"/>


                                    </xsl:apply-templates>

                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:apply-templates select="current-group()/reportmodule[position()]/resultset">

                                        <xsl:sort select="result[position()=$sortByTitlePos1]" order="{$ordering1}"/>
                                        <xsl:sort select="result[position()=$sortByTitlePos2]" order="{$ordering1}"/>

                                        <xsl:with-param name="addindexcolumn"
                                                        select="$addindexcolumn"></xsl:with-param>
                                        <xsl:with-param name="numcols" select="$numcols"/>
                                        <xsl:with-param name="headers" select="$headers"/>
                                        <xsl:with-param name="rpos" select="position()"/>
                                        <xsl:with-param name="resultsetcount" select="$tableid"/>


                                    </xsl:apply-templates>

                                </xsl:otherwise>

                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:choose>
                                <xsl:when
                                        test="$headers/resultsetheader[position()=$sortByTitlePos1 and @sort='false']">
                                    <xsl:apply-templates select="resultset[count(result)!=count(result[@span='0'])]">

                                        <xsl:with-param name="addindexcolumn"
                                                        select="$addindexcolumn"></xsl:with-param>
                                        <xsl:with-param name="numcols" select="$numcols"/>
                                        <xsl:with-param name="headers" select="$headers"/>
                                        <xsl:with-param name="rpos" select="position()"/>
                                        <xsl:with-param name="resultsetcount" select="$tableid"/>

                                    </xsl:apply-templates>
                                </xsl:when>
                                <xsl:when
                                        test="$headers/resultsetheader[position()=$sortByTitlePos1 and @onlyNumericValues='true']">
                                    <xsl:apply-templates select="resultset[count(result)!=count(result[@span='0'])]">

                                        <xsl:sort select="result[position()=$sortByTitlePos1]" order="{$ordering1}"
                                                  data-type="number"/>
                                        <xsl:sort select="result[position()=$sortByTitlePos2]" order="{$ordering1}"/>
                                        <xsl:with-param name="addindexcolumn"
                                                        select="$addindexcolumn"></xsl:with-param>
                                        <xsl:with-param name="numcols" select="$numcols"/>
                                        <xsl:with-param name="headers" select="$headers"/>
                                        <xsl:with-param name="rpos" select="position()"/>
                                        <xsl:with-param name="resultsetcount" select="$tableid"/>
                                        <xsl:with-param name="numListInSeperateColumn"
                                                        select="$numListInSeperateColumn"/>


                                    </xsl:apply-templates>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:apply-templates select="resultset[count(result)!=count(result[@span='0'])]">

                                        <xsl:sort select="result[position()=$sortByTitlePos1]" order="{$ordering1}"/>
                                        <xsl:sort select="result[position()=$sortByTitlePos2]" order="{$ordering1}"/>
                                        <xsl:with-param name="addindexcolumn"
                                                        select="$addindexcolumn"></xsl:with-param>
                                        <xsl:with-param name="numcols" select="$numcols"/>
                                        <xsl:with-param name="headers" select="$headers"/>
                                        <xsl:with-param name="rpos" select="position()"/>
                                        <xsl:with-param name="resultsetcount" select="$tableid"/>
                                        <xsl:with-param name="numListInSeperateColumn"
                                                        select="$numListInSeperateColumn"/>


                                    </xsl:apply-templates>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:otherwise>

                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="handleGrouping">
                        <xsl:with-param name="groupByNodes" select="groupBy"/>
                        <xsl:with-param name="reportmodulegroup" select="."/>
                        <xsl:with-param name="resultsetcount" select="$tableid"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </table>
    </xsl:template>
    <!-- print report header -->
    <xsl:template name="writeheader">
        <table class="reportTableStyle">
            <colgroup span="3">
            </colgroup>
            <tr class='topRow'>
                <!-- Problem here, this needs to be OS dependant OR we make the image available on the web so that it doesn&apos;t refer to a file system object -->
                <td>
                    <b/>
                </td>
                <td>
                    <table>
                        <colgroup span="2">
                        </colgroup>
                        <tr>
                            <td>
                                <b>Report</b>
                            </td>
                            <td>
                                <p>
                                    <xsl:value-of select="@title"/>
                                </p>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <b>User</b>
                            </td>
                            <td>
                                <i>
                                    <xsl:value-of select="$username"/>
                                </i>
                            </td>
                        </tr>
                    </table>
                </td>
                <td>

                    <b/>
                </td>
            </tr>
        </table>
    </xsl:template>
    <!-- print
        resultsubset as comma separated result values-->
    <xsl:template match="resultsubset" mode="as-csv">
        <xsl:param name="symbolView">false</xsl:param>
        <xsl:param name="symbolWidth" as="xs:integer"/>
        <xsl:param name="symbolHeight" as="xs:integer"/>
        <xsl:param name="imageFormat">svg</xsl:param>
        <xsl:if test="$csvmode='true'">
            <xsl:apply-templates mode="as-csv">
                <xsl:sort select="."/>
            </xsl:apply-templates>
        </xsl:if>
        <xsl:if test="$csvmode='false'">
            <xsl:choose>
                <xsl:when test="$symbolView='true'">
                    <xsl:call-template name="symbolViewResult">
                        <xsl:with-param name="symbolView" select="$symbolView"/>
                        <xsl:with-param name="symbolWidth" select="$symbolWidth" as="xs:integer"/>
                        <xsl:with-param name="symbolHeight" select="$symbolHeight" as="xs:integer"/>
                        <xsl:with-param name="imageFormat" select="$imageFormat"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates mode="as-table">
                        <xsl:sort select="."/>

                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:if>


    </xsl:template>
    <xsl:template match="resultsubset" mode="no-sort">
        <xsl:param name="symbolView"/>
        <xsl:param name="symbolWidth" as="xs:integer"/>
        <xsl:param name="symbolHeight" as="xs:integer"/>
        <xsl:param name="imageFormat">svg</xsl:param>
        <xsl:if test="$symbolView='true'">
            <xsl:call-template name="symbolViewResult">
                <xsl:with-param name="symbolView" select="$symbolView"/>
                <xsl:with-param name="symbolWidth" select="$symbolWidth" as="xs:integer"/>
                <xsl:with-param name="symbolHeight" select="$symbolHeight" as="xs:integer"/>
                <xsl:with-param name="imageFormat" select="$imageFormat"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="$symbolView='false'">
            <xsl:if test="$csvmode='true'">
                <xsl:apply-templates mode="as-csv">

                </xsl:apply-templates>
            </xsl:if>
            <xsl:if test="$csvmode='false'">
                <xsl:apply-templates mode="as-table">

                </xsl:apply-templates>
            </xsl:if>
        </xsl:if>

    </xsl:template>
    <xsl:template match="result" mode="as-table">
        <table>
            <tr>
                <td>
                    <xsl:if test='@id != ""'>
                        <xsl:attribute name="class">clickable-multivalued</xsl:attribute>
                        <xsl:variable name="vLinksParts" select="tokenize(@id,',')" as="xs:string*"/>
                        <xsl:variable name="num" select="count($vLinksParts)"/>
                        <xsl:if test="$num = 1">
                            <span>
                                <xsl:attribute name="id">
                                    <xsl:value-of select="@id"/>
                                </xsl:attribute>
                            </span>
                        </xsl:if>

                        <xsl:if test="$num != 1">

                            <xsl:for-each select="1 to ( xs:integer($num))">

                                <xsl:variable name="vIndex" select="(.)" as="xs:integer"/>
                                <span>
                                    <xsl:attribute name="id">
                                        <xsl:value-of select="$vLinksParts[$vIndex]"/>
                                    </xsl:attribute>
                                </span>
                            </xsl:for-each>
                        </xsl:if>
                    </xsl:if>
                    <xsl:value-of select="."/>
                </td>
            </tr>
        </table>
        <xsl:text></xsl:text>
    </xsl:template>
    <xsl:template match="result" mode="as-csv">
        <xsl:variable name="index">
            <xsl:value-of select="position()"/>
        </xsl:variable>
        <xsl:value-of select="concat('[', $index, ']')"/>
        <xsl:value-of select="."/>
        <xsl:if test="position()!=last()">
            <xsl:text>,</xsl:text>
        </xsl:if>
    </xsl:template>

    <xsl:template name="symbolViewResult">
        <xsl:param name="symbolView"/>
        <xsl:param name="symbolWidth" as="xs:integer">
            <xsl:value-of select="$defaultImageWidth"/>
        </xsl:param>

        <xsl:param name="symbolHeight" as="xs:integer">
            <xsl:value-of select="$defaultImageHeight"/>
        </xsl:param>
        <xsl:param name="imageFormat">svg</xsl:param>
        <table>
            <tr>
                <xsl:apply-templates mode="as-symbolview">
                    <xsl:with-param name="symbolView" select="$symbolView"/>
                    <xsl:with-param name="symbolWidth" as="xs:integer" select="$symbolWidth"/>
                    <xsl:with-param name="symbolHeight" as="xs:integer" select="$symbolHeight"/>
                    <xsl:with-param name="imageFormat" select="$imageFormat"/>
                    <xsl:sort select="."/>
                </xsl:apply-templates>
            </tr>
        </table>
    </xsl:template>

    <xsl:template match="result" mode="as-symbolview">
        <xsl:param name="symbolView"/>
        <xsl:param name="symbolWidth" as="xs:integer">
            <xsl:value-of select="$defaultImageWidth"/>
        </xsl:param>
        <xsl:param name="symbolHeight" as="xs:integer">
            <xsl:value-of select="$defaultImageHeight"/>
        </xsl:param>
        <xsl:param name="imageFormat">svg</xsl:param>
        <td>
            <xsl:if test="$symbolView='true'">
                <xsl:variable name="src">
                    <xsl:value-of select="."/>
                </xsl:variable>
                <xsl:variable name="svgPath">
                    <xsl:choose>
                        <xsl:when test="$svglocation='notinitialized'">
                            <!--Use the provide path only-->
                            <xsl:choose>
                                <xsl:when test="$imageFormat='png'">
                                    <xsl:value-of select="concat($src, '.png')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="concat($src, '.svg')"/>
                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:choose>
                                <xsl:when test="$imageFormat='png'">
                                    <xsl:value-of select="concat($svglocation, $src, '.png')"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="concat($svglocation, $src, '.svg')"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:otherwise>
                    </xsl:choose>


                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$bgcolor='black'">
                        <xsl:choose>
                            <xsl:when test="$imageFormat='png'">
                                <img bgcolor="#000000" style="background-color:black" src="{$svgPath}"
                                     width="{$symbolWidth}" height="{$symbolHeight}"/>

                            </xsl:when>
                            <xsl:otherwise>
                                <embed bgcolor="#000000" style="background-color:black; overflow:hidden"
                                       type="image/svg+xml"
                                       src="{$svgPath}" width="{$symbolWidth}" height="{$symbolHeight}"/>
                            </xsl:otherwise>
                        </xsl:choose>

                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="$imageFormat='png'">
                                <img src="{$svgPath}" width="{$symbolWidth}" height="{$symbolHeight}"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <embed type="image/svg+xml" src="{$svgPath}" width="{$symbolWidth}"
                                       height="{$symbolHeight}" style="overflow:hidden"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>

        </td>
        <xsl:if test="position()!=last()">
        </xsl:if>

    </xsl:template>
    <!-- print result or  resultsubset  -->
    <xsl:template name="printresult">
        <xsl:param name="groupByColumnPos">-1</xsl:param>
        <xsl:param name="headers"/>
        <xsl:variable name="rpos">
            <xsl:value-of select="position()"/>
        </xsl:variable>
        <xsl:variable name="curResult">
            <xsl:value-of select="."/>
        </xsl:variable>
        <!-- is type specified as float?-->
        <xsl:variable name="isfloat" as="xs:boolean">
            <xsl:choose>
                <xsl:when test="$headers/resultsetheader[position()=$rpos and @type='float']">
                    <xsl:value-of select="true()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="false()"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="precision" as="xs:integer">
            <xsl:choose>
                <xsl:when test="$headers/resultsetheader[position()=$rpos]/@precision">
                    <xsl:value-of select="$headers/resultsetheader[position()=$rpos]/@precision"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="-1"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="dedecimalseperatorReq" as="xs:boolean">
            <xsl:choose>
                <xsl:when test="$decimalseperator='deDecimal'">
                    <xsl:value-of select="true()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="false()"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="symbolView">
            <xsl:choose>
                <xsl:when test="$headers/resultsetheader[position()=$rpos and @symbolView='true']">
                    <xsl:text>true</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>false</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="symbolHeight" as="xs:integer">
            <xsl:choose>
                <xsl:when test="$headers/resultsetheader[position()=$rpos and @symbolView='true' and @imageHeight]">
                    <xsl:value-of select="$headers/resultsetheader[position()=$rpos]/@imageHeight"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$defaultImageHeight"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="symbolWidth" as="xs:integer">
            <xsl:choose>
                <xsl:when test="$headers/resultsetheader[position()=$rpos and @symbolView='true' and @imageWidth]">
                    <xsl:value-of select="$headers/resultsetheader[position()=$rpos]/@imageWidth"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$defaultImageWidth"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="imageFormat">
            <xsl:choose>
                <xsl:when test="$headers/resultsetheader[position()=$rpos and @symbolView='true' and @imageFormat]">
                    <xsl:value-of select="$headers/resultsetheader[position()=$rpos]/@imageFormat"/>
                </xsl:when>
                <xsl:otherwise>svg</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name='zero'>0,0</xsl:variable>
        <xsl:choose>

            <xsl:when test="$headers/resultsetheader[position()=$rpos]/@accumulateContent">
                <!--<xsl:attribute name="data-accumulateContent">qty</xsl:attribute>-->
                <xsl:choose>
                    <xsl:when test="$headers/resultsetheader[position()=$rpos]/@concatenateStringValues">
                        <xsl:choose>
                            <xsl:when test="current-group()/result[position()=$rpos]/resultsubset">

                                <xsl:for-each select="current-group()/result[position()=$rpos]/resultsubset/result">
                                    <xsl:sort order="ascending"/>
                                    <table>
                                        <tr>
                                            <td>
                                                <xsl:value-of select="."/>
                                            </td>
                                        </tr>
                                    </table>


                                </xsl:for-each>


                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:choose>
                                    <xsl:when
                                            test="$headers/resultsetheader[position()=$rpos]/@concatenateStringValues = 'noseperator'">
                                        <xsl:value-of select="current-group()/result[position()=$rpos]"/>
                                    </xsl:when>

                                    <xsl:otherwise>
                                        <xsl:value-of select="current-group()/result[position()=$rpos]" separator=", "/>

                                                        <xsl:text>
                                                        </xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:otherwise>

                        </xsl:choose>

                    </xsl:when>
                    <xsl:otherwise>

                        <!-- sum all preceding resultset/results whose column position is same as current column -->
                        <xsl:variable name="total" select="sum(current-group()/result[(position()=$rpos) and text()])"/>
                        <xsl:if test="$isfloat">
                            <xsl:variable name="truncatedTotal">
                                <xsl:choose>
                                    <xsl:when test="($precision ne -1)">
                                        <xsl:value-of
                                                select="format-number($total,concat('0.', substring('#######################',1,$precision)))"/>
                                    </xsl:when>
                                    <xsl:when test="$total &lt; 0.1 and $total &gt; -0.1">
                                        <xsl:value-of select="$total"/>
                                    </xsl:when>
                                    <xsl:when test="($total &lt; 0)">
                                        <xsl:value-of
                                                select="(ceiling($total * 100) div 100)"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of
                                                select="(floor($total * 100) div 100)"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <xsl:if test="$dedecimalseperatorReq">
                                <xsl:choose>
                                    <xsl:when test="$curResult = 0">
                                        <xsl:value-of select="$zero"/>
                                    </xsl:when>
                                    <xsl:otherwise>

                                        <xsl:variable name="stringValForTruncatedtotal">
                                            <xsl:value-of select="$truncatedTotal"/>
                                        </xsl:variable>
                                        <xsl:choose>
                                            <xsl:when
                                                    test="contains($stringValForTruncatedtotal, 'E') or contains($stringValForTruncatedtotal,'e')">
                                                <!-- if exponential form -->
                                                <xsl:value-of select="$stringValForTruncatedtotal"/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:choose>
                                                    <xsl:when test="$quotedval='true'">
                                                        <xsl:variable name="temp">
                                                            <xsl:value-of
                                                                    select="format-number($truncatedTotal, '#################0,##########', 'de-German')"/>
                                                        </xsl:variable>
                                                        <xsl:value-of
                                                                select="concat(&apos;,&apos;,$temp,&apos;,&apos;)"/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:value-of
                                                                select="format-number($truncatedTotal , '#################0,##########', 'de-German')"/>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                            <xsl:if test="not($dedecimalseperatorReq)">
                                <xsl:value-of
                                        select="$truncatedTotal"/>
                            </xsl:if>
                        </xsl:if>
                        <xsl:if test="not($isfloat)">
                            <xsl:if test="$total=0">
                                <xsl:text>-</xsl:text>
                            </xsl:if>
                            <xsl:if test="$total!=0">
                                <xsl:value-of select="$total"/>
                            </xsl:if>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
            <xsl:when test="$rpos =  $groupByColumnPos and resultsubset">
                <xsl:value-of select="current-grouping-key()"/>
            </xsl:when>
            <xsl:when test="resultsubset">
                <xsl:if test="not($headers/resultsetheader[position()=$rpos]/@sort)">
                    <xsl:apply-templates select="resultsubset" mode="as-csv">
                        <xsl:with-param name="symbolView" select="$symbolView"/>
                        <xsl:with-param name="symbolWidth" select="$symbolWidth"/>
                        <xsl:with-param name="symbolHeight" select="$symbolHeight"/>
                        <xsl:with-param name="imageFormat" select="$imageFormat"/>
                    </xsl:apply-templates>
                </xsl:if>
                <xsl:if test="$headers/resultsetheader[position()=$rpos]/@sort='false'">
                    <xsl:apply-templates select="resultsubset" mode="no-sort">
                        <xsl:with-param name="symbolView" select="$symbolView"/>
                        <xsl:with-param name="symbolWidth" select="$symbolWidth"/>
                        <xsl:with-param name="symbolHeight" select="$symbolHeight"/>
                        <xsl:with-param name="imageFormat" select="$imageFormat"/>
                    </xsl:apply-templates>

                </xsl:if>
                <xsl:if test="$headers/resultsetheader[position()=$rpos]/@sort = 'true'">
                    <xsl:apply-templates select="resultsubset" mode="as-csv">
                        <xsl:with-param name="symbolView" select="$symbolView"/>
                        <xsl:with-param name="symbolWidth" select="$symbolWidth"/>
                        <xsl:with-param name="symbolHeight" select="$symbolHeight"/>
                        <xsl:with-param name="imageFormat" select="$imageFormat"/>
                    </xsl:apply-templates>
                </xsl:if>
            </xsl:when>


            <!-- round off to two decimal places if current value is a float-->
            <xsl:when test="$isfloat and number($curResult)">
                <xsl:variable name="truncatedCurResult">
                    <xsl:choose>
                        <xsl:when test="($precision ne -1)">
                            <xsl:value-of
                                    select="format-number($curResult,concat('0.', substring('#######################',1,$precision)))"/>
                        </xsl:when>
                        <xsl:when test="($curResult &lt; 0.1 and $curResult &gt; -0.1)">
                            <xsl:value-of select="$curResult"/>
                        </xsl:when>
                        <xsl:when test="($curResult &lt; 0)">
                            <xsl:value-of
                                    select="(ceiling($curResult * 100) div 100)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of
                                    select="(floor($curResult * 100) div 100)"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="stringValFortruncatedCurResult">
                    <xsl:value-of select="$truncatedCurResult"/>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when
                            test="$dedecimalseperatorReq and (not(contains($stringValFortruncatedCurResult, 'E') or contains($stringValFortruncatedCurResult, 'e')))">
                        <xsl:value-of
                                select="format-number($truncatedCurResult, '#################0,##########', 'de-German')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of
                                select="$truncatedCurResult"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>

            <xsl:otherwise>
                <!-- print value as is-->
                <xsl:choose>

                    <xsl:when
                            test="$headers/resultsetheader[position()=$rpos]/@type and ($curResult) and ($curResult!='')">
                        <!--if the result is not empty and type is float-->
                        <xsl:choose>

                            <xsl:when
                                    test="($isfloat and $dedecimalseperatorReq)">
                                <xsl:choose>
                                    <xsl:when test="contains($curResult,'E') or contains($curResult,'e')">
                                        <xsl:value-of select="$curResult"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:choose>
                                            <xsl:when test="$curResult = '0' or $curResult = '0.0'">
                                                <xsl:choose>
                                                    <xsl:when test="$quotedval='true'">
                                                        <xsl:variable name="temp">
                                                            <xsl:value-of select="$zero"/>
                                                        </xsl:variable>
                                                        <xsl:choose>
                                                            <xsl:when test="$shoulddisableoutputescaping='no'">
                                                                <xsl:value-of disable-output-escaping="no"
                                                                              select="concat(&apos;,&apos;,$temp,&apos;,&apos;)"/>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:value-of disable-output-escaping="yes"
                                                                              select="concat(&apos;,&apos;,$temp,&apos;,&apos;)"/>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:value-of select="$zero"/>
                                                    </xsl:otherwise>
                                                </xsl:choose>

                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:choose>
                                                    <xsl:when test="$quotedval='true'">
                                                        <xsl:variable name="temp">
                                                            <xsl:value-of select="."/>
                                                        </xsl:variable>
                                                        <xsl:value-of
                                                                select="concat(&apos;,&apos;,$temp,&apos;,&apos;)"/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:otherwise>
                                </xsl:choose>

                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="."/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when
                                    test="$headers/resultsetheader[position()=$rpos and @disableOutputEscaping='yes']">

                                <xsl:value-of disable-output-escaping="yes" select="."/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="."/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
                <!--<xsl:if test="number($curResult) != NaN">
                                <xsl:if test="$decimalseperator='deDecimal'">
                                        <xsl:value-of
                                                select="format-number($curResult, '##########,##########', 'de-German')"/>
                                </xsl:if>
                                <xsl:if test="$decimalseperator='default'">
                                    <xsl:value-of select="."/>
                                </xsl:if>
                            </xsl:if>
                            <xsl:if test="number($curResult) = NaN">
                                <xsl:value-of select="."/>
                            </xsl:if>-->
            </xsl:otherwise>

        </xsl:choose>
    </xsl:template>
    <!-- handle grouping -->
    <xsl:template name="handleGrouping">

        <xsl:param name="groupByNodes"/>
        <xsl:param name="reportmodulegroup"/>
        <xsl:param name="showColumnHeader" select="true()"/>
        <xsl:param name="resultsetcount"/>
        <xsl:variable name="otherGroupByNodes">

            <xsl:for-each select="$groupByNodes">
                <xsl:if test="position()!=1">
                    <xsl:copy-of select="."/>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="headers">
            <xsl:copy-of select="$reportmodulegroup/resultsetheader"/>
        </xsl:variable>
        <xsl:variable name="gb">
            <xsl:value-of select="$groupByNodes[1]/@title"/>
        </xsl:variable>

        <xsl:variable name="groupByColumnPos">
            <xsl:for-each select="$headers/resultsetheader">
                <xsl:if test="@title = $gb">
                    <xsl:number value="position()"/>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="numListInSeperateColumn">
            <xsl:value-of select="count($headers/resultsetheader[(@listInSeperateColumn)])"></xsl:value-of>
        </xsl:variable>
        <xsl:variable name="numcols">
            <xsl:choose>
                <xsl:when test="$numListInSeperateColumn &gt; 0">
                    <xsl:value-of select="xs:integer($numListInSeperateColumn + 1)"></xsl:value-of>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when
                                test="count($headers/resultsetheader[not(@spanAllCols) and not(@invisible)]) &gt; 0">
                            <xsl:value-of
                                    select="count($headers/resultsetheader[not(@spanAllCols) and not(@invisible)])"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>1</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:variable>


        <xsl:if test="number($groupByColumnPos) &gt; count($headers/resultsetheader)">
            <p>
                could not find column to groupBy:
                <xsl:value-of select="$gb"/>
            </p>
        </xsl:if>
        <xsl:choose>
            <xsl:when test="$groupByNodes[1]/@showGroup">
                <xsl:for-each-group select="$reportmodulegroup/resultset"
                                    group-by="result[position()=$groupByColumnPos and not(child::resultsubset)] | result[position()=$groupByColumnPos]/resultsubset/result">
                    <xsl:sort select="current-grouping-key()"/>

                    <tr class='titleRow'>
                        <td>
                            <xsl:value-of select="$gb"/>
                            :
                            <xsl:choose>
                                <xsl:when
                                        test="$headers/resultsetheader[position() = $groupByColumnPos and @disableOutputEscaping='yes']">
                                    <xsl:value-of disable-output-escaping="yes" select="current-grouping-key()"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="current-grouping-key()"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </td>
                    </tr>
                    <xsl:if test="$showColumnHeader">
                        <xsl:call-template name="header">
                            <xsl:with-param name="reportmodulegroup" select=".."/>
                            <xsl:with-param name="colspan" as="xs:integer" select="$numcols"></xsl:with-param>
                        </xsl:call-template>
                    </xsl:if>
                    <xsl:variable name="subgroup">
                        <xsl:copy-of select="$headers/resultsetheader"/>
                        <xsl:copy-of select="current-group()"/>
                    </xsl:variable>
                    <xsl:choose>
                        <xsl:when test="count($otherGroupByNodes/child::node()) &gt; 0">
                            <xsl:call-template name="handleGrouping">
                                <xsl:with-param name="groupByNodes" select="$otherGroupByNodes/groupBy"/>
                                <xsl:with-param name="reportmodulegroup" select="$subgroup"/>
                                <xsl:with-param name="showColumnHeader" select="not(true())">
                                </xsl:with-param>
                                <xsl:with-param name="resultsetcount" select="$resultsetcount"/>
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>

                            <xsl:apply-templates select="current-group()">
                                <xsl:sort select="result[1]"/>
                                <xsl:with-param name="numcols" select="$numcols"/>
                                <xsl:with-param name="headers" select="$headers"/>
                            </xsl:apply-templates>
                        </xsl:otherwise>
                    </xsl:choose>

                </xsl:for-each-group>
            </xsl:when>
            <xsl:otherwise>
                <!-- only a representative element of each group will be shown-->
                <xsl:if test="$showColumnHeader">
                    <xsl:call-template name="header">
                        <xsl:with-param name="colspan" as="xs:integer" select="$numcols"></xsl:with-param>
                    </xsl:call-template>
                </xsl:if>
                <xsl:choose>
                    <!--when the values in the first column are only numbers then sort by numerics-->
                    <xsl:when
                            test="$headers/resultsetheader[position()=$groupByColumnPos and @sortreportongroupingkey='false']">
                        <xsl:choose>
                            <xsl:when
                                    test="$headers/resultsetheader[position()=1 and @onlyNumericValues='true']">
                                <xsl:for-each-group select="$reportmodulegroup/resultset"
                                                    group-by="result[position()=$groupByColumnPos and not(child::resultsubset)] | result[position()=$groupByColumnPos]/resultsubset/result">


                                    <xsl:sort select="current-group()/result[1]/text()" order="ascending"
                                              data-type="number"/>

                                    <xsl:call-template name="printrow"> <!-- resultset -->
                                        <xsl:with-param name="row" select="current-group()/resultset[1]"/>
                                        <xsl:with-param name="numcols" select="$numcols"/>

                                        <xsl:with-param name="groupByColumnPos" select="$groupByColumnPos"/>
                                        <xsl:with-param name="headers" select="$headers"/>
                                        <xsl:with-param name="resultsetpos" select="position()"/>
                                        <xsl:with-param name="resultsetcount" select="$resultsetcount"/>
                                        <xsl:with-param name="numListInSeperateColumn"
                                                        select="$numListInSeperateColumn"/>
                                    </xsl:call-template>
                                </xsl:for-each-group>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:for-each-group select="$reportmodulegroup/resultset"
                                                    group-by="result[position()=$groupByColumnPos and not(child::resultsubset)] | result[position()=$groupByColumnPos]/resultsubset/result">


                                    <xsl:sort select="current-group()/result[1]/text()" order="ascending"/>

                                    <xsl:call-template name="printrow"> <!-- resultset -->
                                        <xsl:with-param name="row" select="current-group()/resultset[1]"/>
                                        <xsl:with-param name="numcols" select="$numcols"/>
                                        <xsl:with-param name="groupByColumnPos" select="$groupByColumnPos"/>
                                        <xsl:with-param name="headers" select="$headers"/>
                                        <xsl:with-param name="resultsetpos" select="position()"/>
                                        <xsl:with-param name="resultsetcount" select="$resultsetcount"/>
                                        <xsl:with-param name="numListInSeperateColumn"
                                                        select="$numListInSeperateColumn"/>
                                    </xsl:call-template>
                                </xsl:for-each-group>

                            </xsl:otherwise>
                        </xsl:choose>

                    </xsl:when>

                    <xsl:when
                            test="$headers/resultsetheader[position()=$groupByColumnPos and @onlyNumericValues='true']">
                        <xsl:for-each-group select="$reportmodulegroup/resultset"
                                            group-by="result[position()=$groupByColumnPos and not(child::resultsubset)] | result[position()=$groupByColumnPos]/resultsubset/result">
                            <xsl:sort select="current-grouping-key()" order="ascending" data-type="number"/>
                            <xsl:call-template name="printrow"> <!-- resultset -->
                                <xsl:with-param name="row" select="current-group()/resultset[1]"/>
                                <xsl:with-param name="numcols" select="$numcols"/>
                                <xsl:with-param name="groupByColumnPos" select="$groupByColumnPos"/>
                                <xsl:with-param name="headers" select="$headers"/>
                                <xsl:with-param name="resultsetpos" select="position()"/>
                                <xsl:with-param name="resultsetcount" select="$resultsetcount"/>
                                <xsl:with-param name="numListInSeperateColumn" select="$numListInSeperateColumn"/>
                            </xsl:call-template>

                        </xsl:for-each-group>
                    </xsl:when>
                    <xsl:otherwise>

                        <xsl:for-each-group select="$reportmodulegroup/resultset"
                                            group-by="result[position()=$groupByColumnPos and not(child::resultsubset)] | result[position()=$groupByColumnPos]/resultsubset/result">
                            <xsl:sort select="current-grouping-key()" order="ascending"/>
                            <xsl:call-template name="printrow"> <!-- resultset -->
                                <xsl:with-param name="row" select="current-group()/resultset[1]"/>
                                <xsl:with-param name="numcols" select="$numcols"/>
                                <xsl:with-param name="groupByColumnPos" select="$groupByColumnPos"/>
                                <xsl:with-param name="headers" select="$headers"/>
                                <xsl:with-param name="resultsetpos" select="position()"/>
                                <xsl:with-param name="resultsetcount" select="$resultsetcount"/>
                                <xsl:with-param name="numListInSeperateColumn" select="$numListInSeperateColumn"/>
                            </xsl:call-template>

                        </xsl:for-each-group>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="resultset">
        <xsl:param name="numcols">0</xsl:param>
        <xsl:param name="addindexcolumn"/>
        <xsl:param name="resultsetpos">
            <xsl:value-of select="position()"/>
        </xsl:param>
        <xsl:param name="groupByColumnPos" as="xs:integer">-1</xsl:param>
        <xsl:param name="headers"/>
        <xsl:param name="resultsetcount"/>
        <xsl:param name="numListInSeperateColumn">0</xsl:param>
        <xsl:call-template name="printrow">
            <xsl:with-param name="numcols" select="$numcols"/>
            <xsl:with-param name="addindexcolumn"
                            select="$addindexcolumn"/>
            <xsl:with-param name="resultsetpos" select="$resultsetpos"/>
            <xsl:with-param name="groupByColumnPos" select="$groupByColumnPos"/>
            <xsl:with-param name="headers" select="$headers"/>
            <xsl:with-param name="resultsetcount" select="$resultsetcount"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="printrow">
        <xsl:param name="numcols">0</xsl:param>
        <xsl:param name="addindexcolumn"/>
        <xsl:param name="resultsetpos">
            <xsl:value-of select="position()"/>
        </xsl:param>
        <xsl:param name="groupByColumnPos" as="xs:integer">-1</xsl:param>
        <xsl:param name="headers"/>
        <xsl:param name="row" select="."/>
        <xsl:param name="resultsetcount">
        </xsl:param>
        <xsl:param name="numListInSeperateColumn">0</xsl:param>
        <tr>
            <xsl:attribute name="style">
                <xsl:text>white-space: pre-wrap;</xsl:text>
            </xsl:attribute>

            <xsl:attribute name="style">
                <xsl:text>white-space: pre-wrap;</xsl:text>
            </xsl:attribute>

            <xsl:variable name="firstResultRowSpan" select="result[1]/@span"/>
            <!-- append a row span attribute only if some column in this row has a different
                             row span than others-->
            <xsl:variable name="showRowSpan"
                          select="count(result[@span=$firstResultRowSpan])!=count(result)"/>

            <xsl:call-template name="addIndexColumn">

                <xsl:with-param name="resultsetpos"
                                select="$resultsetpos"/>
                <xsl:with-param name="addindexcolumn"
                                select="$addindexcolumn"/>
            </xsl:call-template>

            <xsl:apply-templates select="result" mode="single-colspan">
                <xsl:with-param name="groupByColumnPos" as="xs:integer" select="$groupByColumnPos"/>
                <xsl:with-param name="headers" select="$headers"/>
                <xsl:with-param name="showRowSpan" select="$showRowSpan"/>
            </xsl:apply-templates>
        </tr>
        <xsl:choose>
            <xsl:when
                    test="$numListInSeperateColumn &gt;'0'"> <!--when the listInSeperateColumnPresent is specified for any of the report headers-->

                <xsl:variable name="totlaNumberOfColumns" as="xs:integer"
                              select="xs:integer($numListInSeperateColumn + 1)">
                    <!-- apply floor of to get the integer return value instead of double-->

                </xsl:variable>

                <!--tr style="border-top: black thick solid; background-color: #efefef"-->
                <tr>
                    <td valign="top" colspan="1">
                        <xsl:attribute name="width">
                            <xsl:value-of select="concat(floor(100 div number($totlaNumberOfColumns)),'%')"/>
                        </xsl:attribute>
                        <table>
                            <xsl:for-each
                                    select="result"> <!--select all the report templates that do not have the listInSeperateColumnPresent-->
                                <xsl:variable name="resultpos">
                                    <xsl:value-of select="position()"/>
                                </xsl:variable>
                                <xsl:variable name="listInSeperateColumnFlag">
                                    <xsl:choose>
                                        <xsl:when
                                                test="$headers/resultsetheader[position()=$resultpos and @listInSeperateColumn=&apos;yes&apos;]">
                                            <text>true</text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <text>false</text>
                                        </xsl:otherwise>
                                    </xsl:choose>

                                </xsl:variable>
                                <xsl:choose>
                                    <xsl:when
                                            test="$listInSeperateColumnFlag='false'">
                                        <xsl:call-template name="resultMultiColumnSpan">
                                            <xsl:with-param name="numcols" as="xs:integer"
                                                            select="$numcols"/>
                                            <xsl:with-param name="headers" select="$headers"/>
                                        </xsl:call-template>

                                    </xsl:when>

                                </xsl:choose>


                            </xsl:for-each>
                        </table>
                    </td>
                    <xsl:for-each select="result">

                        <xsl:variable name="resultpos">
                            <xsl:value-of select="position()"/>
                        </xsl:variable>
                        <xsl:choose>
                            <xsl:when
                                    test="$headers/resultsetheader[position()=$resultpos and @listInSeperateColumn=&apos;yes&apos;]">
                                <td valign="top" align="right" colspan="1"> <!--Colspan is always 1-->
                                    <xsl:attribute name="width">
                                        <xsl:value-of
                                                select="concat(floor(100 div number($totlaNumberOfColumns)),'%')"/>
                                    </xsl:attribute>


                                    <table>
                                        <xsl:call-template name="resultMultiColumnSpan">
                                            <xsl:with-param name="numcols" as="xs:integer"
                                                            select="$numcols"/>
                                            <xsl:with-param name="headers" select="$headers"/>
                                        </xsl:call-template>
                                    </table>


                                </td>
                            </xsl:when>

                        </xsl:choose>


                    </xsl:for-each>
                </tr>

                <!-- handle nested reportmodules here we treat each module as a separate row as if it was a result with spanAllCols set to true-->
                <xsl:apply-templates select="reportmodule" mode="as-row">
                    <xsl:with-param name="numcols" as="xs:integer" select="$totlaNumberOfColumns"/>
                    <xsl:with-param name="resultsetposition" select="$resultsetpos"/>
                    <xsl:with-param name="resultsetcount" select="concat($resultsetcount,'-',position())"/>
                    <xsl:with-param name="groupByColPos" select="$groupByColumnPos"></xsl:with-param>
                </xsl:apply-templates>
            </xsl:when>
            <xsl:otherwise> <!--when none of the report headers have the listInSeperateColumnPresent -->
                <xsl:apply-templates select="result" mode="multi-colspan">
                    <xsl:with-param name="numcols" as="xs:integer" select="$numcols"/>
                    <xsl:with-param name="headers" select="$headers"/>
                </xsl:apply-templates>
                <!-- handle nested reportmodules here we treat each module as a separate row as if it was a result with spanAllCols set to true-->
                <xsl:apply-templates select="reportmodule" mode="as-row">
                    <xsl:with-param name="numcols" as="xs:integer" select="$numcols"/>
                    <xsl:with-param name="resultsetposition" select="$resultsetpos"/>
                    <xsl:with-param name="resultsetcount" select="concat($resultsetcount,'-',position())"/>
                    <xsl:with-param name="groupByColPos" select="$groupByColumnPos"></xsl:with-param>
                </xsl:apply-templates>
            </xsl:otherwise>
        </xsl:choose>


    </xsl:template>
    <xsl:template name="addIndexColumn">
        <xsl:param name="resultsetpos"/>
        <xsl:param name="addindexcolumn"/>
        <xsl:if test="$addindexcolumn != ''">
            <td>
                <xsl:value-of select="$resultsetpos"></xsl:value-of>
            </td>
        </xsl:if>
    </xsl:template>
    <xsl:template match="reportmodule" mode="as-row">
        <xsl:param name="numcols"/>
        <xsl:param name="resultsetposition"/>
        <xsl:param name="resultsetcount"/>
        <xsl:param name="groupByColPos"></xsl:param>
        <tr class='nestedTableRow'>
            <td>
                <xsl:attribute name="colspan">
                    <xsl:value-of select="$numcols"/>
                </xsl:attribute>
                <xsl:apply-templates select=".">
                    <xsl:with-param name="tableindex">tableid</xsl:with-param>
                    <xsl:with-param name="resultsetcount" select="concat($resultsetcount,'-',position())"/>
                    <xsl:with-param name="groupByColPos" select="$groupByColPos"/>
                </xsl:apply-templates>
            </td>
        </tr>
    </xsl:template>
    <xsl:template name="resultMultiColumnSpan">
        <!-- handle results with spanAllCols set to true -->
        <xsl:param name="numcols" as="xs:integer">-1</xsl:param>
        <xsl:param name="headers"/>
        <xsl:variable name="rpos">
            <xsl:value-of select="position()"/>
        </xsl:variable>

        <xsl:if test="$headers/resultsetheader[position()=$rpos and @spanAllCols=&apos;true&apos;] and (not(@span) or (@span &gt; 0))">
            <tr>

                <td colspan="1">

                    <!--	<xsl:attribute name="rowspan">
                                                                                                <xsl:value-of select="@span"/>
                                                                                            </xsl:attribute> -->

                    <xsl:if test="$headers/resultsetheader[position()=$rpos and @spanAllCols=&apos;true&apos;]/@title != ''">
                        <span>
                            <xsl:value-of
                                    select="$headers/resultsetheader[position()=$rpos and @spanAllCols=&apos;true&apos;]/@title"/>
                        </span>
                        <xsl:text>:</xsl:text>
                    </xsl:if>

                    <xsl:call-template name="printresult">
                        <xsl:with-param name="headers" select="$headers"/>
                    </xsl:call-template>

                </td>
            </tr>
        </xsl:if>


    </xsl:template>
    <xsl:template match="result" mode="multi-colspan">
        <!-- handle results with spanAllCols set to true -->
        <xsl:param name="numcols" as="xs:integer">-1</xsl:param>
        <xsl:param name="headers"/>
        <xsl:variable name="rpos">
            <xsl:value-of select="position()"/>
        </xsl:variable>

        <xsl:if test="$headers/resultsetheader[position()=$rpos and @spanAllCols=&apos;true&apos;] and (not(@span) or (@span &gt; 0))">
            <tr>
                <td>
                    <xsl:attribute name="colspan">
                        <xsl:value-of select="$numcols"/>
                    </xsl:attribute>
                    <!--	<xsl:attribute name="rowspan">
                                                                            <xsl:value-of select="@span"/>
                                                                        </xsl:attribute> -->

                    <span>
                        <xsl:value-of
                                select="$headers/resultsetheader[position()=$rpos and @spanAllCols=&apos;true&apos;]/@title"/>
                    </span>
                    <xsl:text>:</xsl:text>
                    <xsl:call-template name="printresult">
                        <xsl:with-param name="headers" select="$headers"/>
                    </xsl:call-template>

                </td>
            </tr>
        </xsl:if>


    </xsl:template>
    <xsl:template match="result" mode="single-colspan">
        <xsl:param name="groupByColumnPos">-1</xsl:param>
        <xsl:param name="headers"/>
        <xsl:param name="currgroupingkey"/>
        <xsl:param name="showRowSpan" select="true()"/>
        <xsl:variable name="rpos">
            <xsl:value-of select="position()"/>
        </xsl:variable>
        <xsl:if test="$headers/resultsetheader[position()=$rpos and not(@spanAllCols) and not(@invisible) ]  and (not(@span) or (@span &gt; 0))">
            <td>
                <xsl:if test="@span and $showRowSpan">
                    <xsl:attribute name="rowspan">
                        <xsl:value-of select="@span"/>
                    </xsl:attribute>
                </xsl:if>

                <xsl:if test='@id != ""'>
                    <xsl:attribute name="class">clickable-column</xsl:attribute>
                   
                        <xsl:if test="$headers/resultsetheader/@accumulateContent">
                            <xsl:for-each select="/report/reportmodule/resultset/result">
                                <xsl:variable name="currresult" select="."/>
                                <xsl:if test="$currgroupingkey = $currresult">
                                    <xsl:if test='@id != ""'>
                                        <xsl:variable name="vLinksParts" select="tokenize(@id,',')" as="xs:string*"/>
                                        <xsl:variable name="num" select="count($vLinksParts)"/>
                                        <xsl:if test="$num = 1">
                                            <span>
                                                <xsl:attribute name="id">
                                                    <xsl:value-of select="@id"/>
                                                </xsl:attribute>
                                            </span>
                                        </xsl:if>

                                        <xsl:if test="$num != 1">

                                            <xsl:for-each select="1 to ( xs:integer($num div 2) + 1)">

                                                <xsl:variable name="vIndex" select="(.)" as="xs:integer"/>
                                                <span>
                                                    <xsl:attribute name="id">
                                                        <xsl:value-of select="$vLinksParts[$vIndex]"/>
                                                    </xsl:attribute>
                                                </span>
                                            </xsl:for-each>
                                        </xsl:if>
                                    </xsl:if>
                                </xsl:if>
                            </xsl:for-each>
                            <xsl:for-each select="/report/reportmodule/resultset/reportmodule/resultset/result">
                                <xsl:variable name="currresult" select="."/>
                                <xsl:if test="$currgroupingkey = $currresult">
                                    <xsl:if test='@id != ""'>
                                        <xsl:variable name="vLinksParts" select="tokenize(@id,',')" as="xs:string*"/>
                                        <xsl:variable name="num" select="count($vLinksParts)"/>
                                        <xsl:if test="$num = 1">
                                            <span>
                                                <xsl:attribute name="id">
                                                    <xsl:value-of select="@id"/>
                                                </xsl:attribute>
                                            </span>
                                        </xsl:if>

                                        <xsl:if test="$num != 1">

                                            <xsl:for-each select="1 to ( xs:integer($num div 2) + 1)">

                                                <xsl:variable name="vIndex" select="(.)" as="xs:integer"/>
                                                <span>
                                                    <xsl:attribute name="id">
                                                        <xsl:value-of select="$vLinksParts[$vIndex]"/>
                                                    </xsl:attribute>
                                                </span>
                                            </xsl:for-each>
                                        </xsl:if>
                                    </xsl:if>
                                </xsl:if>
                            </xsl:for-each>

                        </xsl:if>
                      
                            <xsl:if test='@id != ""'>
                                <xsl:variable name="vLinksParts" select="tokenize(@id,',')" as="xs:string*"/>
                                <xsl:variable name="num" select="count($vLinksParts)"/>
                                <xsl:if test="$num = 1">
                                    <span>
                                        <xsl:attribute name="id">
                                            <xsl:value-of select="@id"/>
                                        </xsl:attribute>
                                    </span>
                                </xsl:if>

                                <xsl:if test="$num != 1">

                                    <xsl:for-each select="1 to $num">

                                        <xsl:variable name="vIndex" select="(.)" as="xs:integer"/>
                                        <span>
                                            <xsl:attribute name="id">
                                                <xsl:value-of select="$vLinksParts[$vIndex]"/>
                                            </xsl:attribute>
                                        </span>
                                    </xsl:for-each>
                                </xsl:if>
                            </xsl:if>
                       
                    
                </xsl:if>

                <xsl:call-template name="printresult">
                    <xsl:with-param name="groupByColumnPos" select="$groupByColumnPos"/>
                    <xsl:with-param name="headers" select="$headers"/>
                </xsl:call-template>
            </td>
        </xsl:if>

    </xsl:template>
    <xsl:template match="resultsetheader">
        <xsl:param name="colspan"/>
        <td>
            <!--<xsl:attribute name="width">
                <xsl:value-of select="concat(floor(100 div number($colspan)),'%')"/>
            </xsl:attribute>-->
            <xsl:if test="(@title!='' and @showColumnHeader='false')">
                <xsl:variable as="xs:integer" name="length">
                    <xsl:choose>
                        <xsl:when test="@title!=''">
                            <xsl:value-of
                                    select="string-length(@title)"/>
                        </xsl:when>
                    </xsl:choose>
                </xsl:variable>
                <table frame="lhs">
                    <tr>
                        <xsl:call-template name="looping">
                            <xsl:with-param name="index" select="0"/>
                            <xsl:with-param name="length" select="$length"/>
                        </xsl:call-template>
                    </tr>
                </table>
            </xsl:if>
            <xsl:if test="(@title!='' and (not(@showColumnHeader)))">
                <xsl:choose>
                    <xsl:when test="@disableOutputEscaping and (@disableOutputEscaping = 'yes')">
                        <xsl:value-of disable-output-escaping="yes" select="@title"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="@title"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>

        </td>
    </xsl:template>
    <xsl:template name="header">
        <xsl:param name="reportmodulegroup" select="."/>
        <xsl:param name="colspan"/>
        <xsl:param name="addindexcolumn"/>
        <tr>
            <xsl:if test="$addindexcolumn != ''">
                <td>
                    <xsl:value-of select="$addindexcolumn"></xsl:value-of>
                </td>
            </xsl:if>
            <xsl:apply-templates
                    select="$reportmodulegroup/resultsetheader[not(@spanAllCols) and not(@invisible)]">
                <xsl:with-param name="colspan" select="$colspan"/>
            </xsl:apply-templates>
        </tr>
    </xsl:template>

    <xsl:template name="looping">
        <xsl:param name="index"/>
        <xsl:param name="length"/>
        <xsl:if test="$index &lt;= $length">
            <td colspan='2' align='left'></td>
            <xsl:call-template name="looping">
                <xsl:with-param name="index" select="$index + 1"/>
                <xsl:with-param name="length" select="$length"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>