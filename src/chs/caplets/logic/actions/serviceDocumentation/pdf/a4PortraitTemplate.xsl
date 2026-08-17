<?xml version="1.0"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:fo="http://www.w3.org/1999/XSL/Format"
        version="2.0">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <!-- The below defined parameters are set from the application with proper values, do not remove them-->
    <xsl:param name="header" select="''"/>
    <xsl:param name="footer" select="''"/>
    <xsl:param name="startString" select="''"/>
    <xsl:param name="endString" select="''"/>
    <xsl:param name="pageNumber" select="''"/>
    <xsl:template match="/">
        <!-- the units used in this template are in mm.-->
        <!-- Other units which can be used in XSL-FO are cm,in,pt,pc,px,em -->
        <!--http://www.w3.org/TR/2001/REC-xsl-20011015/slice5.html#section-N8185-Definitions-of-Units-of-Measure-->
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <!-- the below template defines height width of the page and margins of the page-->
                <fo:simple-page-master master-name="portrait"
                                       page-height="297mm" page-width="210mm"
                                       margin-top="0mm" margin-bottom="0mm"
                                       margin-left="0mm" margin-right="0mm">
                    <!-- defines the body region of the page-->
                    <fo:region-body/>
                    <!-- defines the region-before which is the header of the body-->
                    <!-- background color attribute can be specified to differentiate different regions-->
                    <!--<fo:region-before region-name="header-normal" extent="10mm" background-color="#cce3e1"/>-->
                    <fo:region-before region-name="header-normal" extent="10mm"/>
                    <!-- defines the region-after which is the footer of the body-->
                    <fo:region-after region-name="footer-normal" extent="10mm"/>
                    <!-- defines the region-start which is the area to the left of the body-->
                    <fo:region-start region-name="start-normal" extent="10mm"/>
                    <!-- defines the region-end which is the area to the rigth of the body-->
                    <fo:region-end region-name="end-normal" extent="10mm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>
            <fo:page-sequence master-reference="portrait">
                <fo:static-content flow-name="header-normal">
                    <!--below static content will go into the header of every page which uses this template-->
                    <fo:block text-align="center" display-align="after" text-decoration="underline" font-family="customfont,Helvetica">
                        <xsl:value-of select="$header"/>
                    </fo:block>
                </fo:static-content>
                <fo:static-content flow-name="footer-normal">
                    <!--below static content will go into the footer of every page which uses this template-->
                    <fo:block text-align="center" font-family="customfont,Helvetica">
                        <xsl:value-of select="$pageNumber"/>
                    </fo:block>
                    <fo:block text-align="right" font-family="customfont,Helvetica">
			<xsl:value-of select="$footer"/>
                        <!--<fo:external-graphic src="H:\chs_home\publisher\pdf\img.gif" content-width="120px" content-height="40px" background-color="white"/>-->
                    </fo:block>
                </fo:static-content>
                <fo:static-content flow-name="start-normal">
                    <!--below static content will go into the start region of every page which uses this template-->
                    <fo:block text-align="center" font-family="customfont,Helvetica">
                        <xsl:value-of select="$startString"/>
                    </fo:block>
                </fo:static-content>
                <fo:static-content flow-name="end-normal">
                    <!--below static content will go into the end region of every page which uses this template-->
                    <fo:block text-align="center" font-family="customfont,Helvetica">
                        <xsl:value-of select="$endString"/>
                    </fo:block>
                </fo:static-content>
                <!--do not add any content in the body, as it would be overriden with the generated content-->
                <fo:flow flow-name="xsl-region-body">
                    <fo:block/>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
</xsl:stylesheet>