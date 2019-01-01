import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import org.apache.commons.lang3.StringUtils

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = ""
basePackageName = ""
idProperties = ["id"] as String[]
gmtCreate = ["gmt_create"] as String[]
gmtModified = ["gmt_modified"] as String[]
isDeleteProperties = ["is_delete"] as String[]
delete = 1
commonProperties = ["id", "gmt_create", "gmt_modified"] as String[]
javaTypeMapping = [
        (~/(?i)bigint/)                   : "Long",
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)/)                         : "String"
]

parameterTypeMapping = [
        (~/(?i)bigint/)                   : "java.lang.Long",
        (~/(?i)int/)                      : "java.lang.Integer",
        (~/(?i)float|double|decimal|real/): "java.lang.Double",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)/)                         : "java.lang.String"
]

sepa = java.io.File.separator

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    int index = dir.toString().lastIndexOf(sepa + "src" + sepa + "main" + sepa + "java" + sepa)
    if (index != -1) {
        packageName = dir.toString().substring(index + 15).replaceAll(sepa, ".")
    }
    int index_last = packageName.lastIndexOf(".")
    if (index_last != -1) {
        basePackageName = packageName.toString().substring(0, index_last)
    }
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    def tableName = table.getName()
    def xmlDir = dir.toString().substring(0, index + 10) + sepa + "resources" + sepa + "mapper" + sepa
    def baseXmlDir = dir.toString().substring(0, index + 10) + sepa + "resources" + sepa + "mapper" + sepa + "base" + sepa
    def baseXmlFile = new File(baseXmlDir)
    baseXmlFile.mkdirs()
    new File(baseXmlDir, className + "BaseMapper.xml").withPrintWriter { out -> baseXml(out, tableName, className, fields) }
    def xmlFile = new File(xmlDir, className + "Mapper.xml")
    if (!xmlFile.exists()) {
        xmlFile.withPrintWriter { out -> xml(out, tableName, className, fields) }
    }
}

def baseXml(out, tableName, className, fields) {
    int index = 0
    out.println "<?xml version='1.0' encoding='UTF-8' ?>"
    out.println "<!DOCTYPE mapper PUBLIC '-//mybatis.org//DTD Mapper 3.0//EN' 'http://mybatis.org/dtd/mybatis-3-mapper.dtd' >"
    out.println "<mapper namespace='${packageName}.mapper.base.${className}BaseMapper'>"
    out.println ""
    out.println "    <resultMap id='BaseResultMap' type='${packageName}.model.${className}Model'>"
    out.println "        <constructor>"
    fields.each() {
        if (propertiesContainField(it, idProperties)) {
            out.println "            <idArg column='${it.colName}' javaType='${it.parameterType}'/>"
        }else {
            out.println "            <arg column='${it.colName}' javaType='${it.parameterType}'/>"
        }
    }
    out.println "        </constructor>"
    fields.each() {
        out.println "        <result property='${it.javaName}' column='${it.colName}'/>"
    }
    out.println "    </resultMap>"
    out.println ""
    out.println "    <sql id='Base_Column_List'>"
    out.print "    "
    fields.each() {
        if (index != 0) {
            out.print ", "
        }
        out.print "${tableName}.${it.colName}"
        index++
    }
    out.println ""
    out.println "    </sql>"
    out.println ""
    out.println "    <select id='selectByPrimaryKey' resultMap='BaseResultMap'"
    out.println "            parameterType='java.lang.Long'>"
    out.println "        select "
    out.println "        <include refid='Base_Column_List'/>"
    out.println "        from ${tableName} "
    out.println "        where ${tableName}.id = #{id}"
    out.println "    </select>"
    fields.each() {
        String str = it.colName
        if (str.endsWith("_id")) {
            def ForeignKey = javaName(it.colName, true)
            def foreignKey = javaName(it.colName, false)
            out.println ""
            out.println "    <select id='selectBy${ForeignKey}' resultMap='BaseResultMap'"
            out.println "            parameterType='java.lang.Long'>"
            out.println "        select "
            out.println "        <include refid='Base_Column_List'/>"
            out.println "        from ${tableName} "
            out.println "        where ${tableName}.${it.colName} = #{${foreignKey}}"
            out.println "    </select>"
        }
    }
    out.println ""
    out.println "    <select id='selectOneByQuery' resultMap='BaseResultMap'"
    out.println "            parameterType='${packageName}.model.${className}Model'>"
    out.println "        select "
    out.println "        <include refid='Base_Column_List'/>"
    out.println "        from ${tableName} "
    out.println "        <where>"
    out.println "            <include refid='query_filter'/>"
    out.println "        </where>"
    out.println "        Limit 1"
    out.println "    </select>"
    out.println ""
    out.println "    <select id='selectByQuery' resultMap='BaseResultMap'"
    out.println "            parameterType='${packageName}.model.${className}Model'>"
    out.println "        select "
    out.println "        <include refid='Base_Column_List'/>"
    out.println "        from ${tableName} "
    out.println "        <where>"
    out.println "            <include refid='query_filter'/>"
    out.println "        </where>"
    out.println "    </select>"
    out.println ""
    if (fieldsContainProperties(isDeleteProperties, fields)) {
        out.println "    <update id='deleteByPrimaryKey' parameterType='java.lang.Long'>"
        out.println "        update ${tableName} set ${tableName}.${isDeleteProperties[0]} = ${delete} where ${tableName}.id = #{id}"
        out.println "    </update>"
        out.println ""
    } else {
        out.println "    <delete id='deleteByPrimaryKey' parameterType='java.lang.Long'>"
        out.println "        delete from ${tableName} where ${tableName}.id = #{id}"
        out.println "    </delete>"
        out.println ""
    }
    fields.each() {
        String str = it.colName
        if (str.endsWith("_id") && str != "target_id") {
            def ForeignKey = javaName(it.colName, true)
            def foreignKey = javaName(it.colName, false)
            if (fieldsContainProperties(isDeleteProperties, fields)) {
                out.println "    <update id='deleteBy${ForeignKey}' parameterType='java.lang.Long'>"
                out.println "        update ${tableName} set ${tableName}.${isDeleteProperties[0]} = ${delete} where ${tableName}.${it.colName} = #{${foreignKey}}"
                out.println "    </update>"
                out.println ""
            } else {
                out.println "    <delete id='deleteBy${ForeignKey}' parameterType='java.lang.Long'>"
                out.println "        delete from ${tableName} where ${tableName}.${it.colName} = #{${foreignKey}}"
                out.println "    </delete>"
                out.println ""
            }
        }
    }
    if (fieldsContainProperties(isDeleteProperties, fields)) {
        out.println "    <update id='deleteByQuery' parameterType='${packageName}.model.${className}Model'>"
        out.println "        update ${tableName} set ${tableName}.${isDeleteProperties[0]} = ${delete}"
        out.println "        <where>"
        out.println "            <include refid='query_filter'/>"
        out.println "        </where>"
        out.println "    </update>"
        out.println ""
    } else {
        out.println "    <delete id='deleteByQuery' parameterType='${packageName}.model.${className}Model'>"
        out.println "        delete from ${tableName}"
        out.println "        <where>"
        out.println "            <include refid='query_filter'/>"
        out.println "        </where>"
        out.println "    </delete>"
        out.println ""
    }
    out.println "    <select id='count' resultType='java.lang.Integer' parameterType='${packageName}.model.${className}Model'>"
    out.println "        select count(*) from ${tableName}"
    out.println "        <where>"
    out.println "            <include refid='query_filter'/>"
    out.println "        </where>"
    out.println "    </select>"
    out.println ""
    out.println "    <insert id='insert' parameterType='${packageName}.model.${className}Model' useGeneratedKeys='true' keyProperty='id'>"
    out.println "        insert into ${tableName}"
    out.println "        <trim prefix='(' suffix=')' suffixOverrides=','>"
    fields.each() {
        if (propertiesContainField(it, gmtCreate)) {
            out.println "            ${tableName}.${it.colName},"
        } else if (propertiesContainField(it, gmtModified)) {
            out.println "            ${tableName}.${it.colName},"
        } else if (propertiesContainField(it, isDeleteProperties)) {
            out.println "            ${tableName}.${it.colName},"
        } else {
            out.println "            <if test='${it.javaName} != null'>${tableName}.${it.colName},</if>"
        }
    }
    out.println "        </trim>"
    out.println "        <trim prefix='values (' suffix=')' suffixOverrides=','>"
    fields.each() {
        if (propertiesContainField(it, gmtCreate)) {
            out.println "            now(),"
        } else if (propertiesContainField(it, gmtModified)) {
            out.println "            now(),"
        } else if (propertiesContainField(it, isDeleteProperties)) {
            out.println "            0,"
        } else {
            out.println "            <if test='${it.javaName} != null'>#{${it.javaName}},</if>"
        }
    }
    out.println "        </trim>"
    out.println "    </insert>"
    out.println ""
    out.println "    <update id='update' parameterType='${packageName}.model.${className}Model'>"
    out.println "        update ${tableName}"
    out.println "        <set>"
    fields.each() {
        if (propertiesContainField(it, gmtCreate)) {
        } else if (propertiesContainField(it, gmtModified)) {
            out.println "            ${tableName}.${it.colName} = now(),"
        } else {
            out.println "            <if test='${it.javaName} != null'>${tableName}.${it.colName} = #{${it.javaName}},</if>"
        }
    }
    out.println "        </set>"
    out.println "        where ${tableName}.id = #{id}"
    out.println "    </update>"
    out.println ""
    out.println "    <sql id='query_filter'>"
    fields.each() {
        if (propertiesContainField(it, isDeleteProperties)) {
            out.println "        and ${tableName}.${it.colName} != ${delete}"
        } else {
            out.println "        <if test='${it.javaName} != null'>and ${tableName}.${it.colName} = #{${it.javaName}}</if>"
        }
    }
    out.println "    </sql>"
    out.println ""
    out.println "</mapper>"
}

def xml(out, tableName, className, fields) {
    out.println "<?xml version='1.0' encoding='UTF-8' ?>"
    out.println "<!DOCTYPE mapper PUBLIC '-//mybatis.org//DTD Mapper 3.0//EN' 'http://mybatis.org/dtd/mybatis-3-mapper.dtd' >"
    out.println "<mapper namespace='${packageName}.mapper.${className}Mapper'>"
    out.println ""
    out.println "</mapper>"
}

boolean fieldsContainProperties(properties, fields) {
    def exist = false
    properties.each() {
        def property = it
        fields.each() {
            if (property == it.right) {
                exist = true
            }
        }
    }
    exist
}

boolean propertiesContainField(field, properties) {
    def exist = false
    properties.each() {
        if (field.right == it) {
            exist = true
        }
    }
    exist
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def javaTypeStr = javaTypeMapping.find { p, t -> p.matcher(spec).find() }.value
        def parameterTypeStr = parameterTypeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           javaName     : javaName(col.getName(), false),
                           colName      : col.getName(),
                           parameterName: parameterName(col.getName(), true),
                           dataType     : col.getDataType(),
                           jdbcType     : col.getDataType(),
                           javaType     : javaTypeStr,
                           parameterType: parameterTypeStr,
                           comment      : col.getComment(),
                           annos        : ""]]
    }
}

def javaName(str, capitalize) {
    String strTmp = str
    if (strTmp.startsWith("is_")) {
        str = str.replaceFirst("is_", "")
    }
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def parameterName(str, capitalize) {
    String strTmp = str
    if (strTmp.startsWith("is_")) {
        str = str.replaceFirst("is_", "")
    }
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def tableName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
