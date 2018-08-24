import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import org.apache.commons.lang3.StringUtils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = ""
gmtCreate = ["gmt_create"] as String[]
gmtModified = ["gmt_modified"] as String[]
isDeleteProperties = ["is_delete"] as String[]
commonProperties = ["id", "gmt_create", "gmt_modified"] as String[]
typeMapping = [
        (~/(?i)bigint/)                   : "Long",
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)/)                         : "String"
]


dataTypeMapping = [
        (~/(?i)bigint/)                   : "integer",
        (~/(?i)int/)                      : "integer",
        (~/(?i)float|double|decimal|real/): "number",
        (~/(?i)datetime|timestamp/)       : "string",
        (~/(?i)date/)                     : "string",
        (~/(?i)time/)                     : "string",
        (~/(?i)/)                         : "string"
]

exampleMapping = [
        (~/(?i)bigint/)                   : "1",
        (~/(?i)int/)                      : "1",
        (~/(?i)float|double|decimal|real/): "1.00",
        (~/(?i)datetime|timestamp/)       : LocalDateTime.now(),
        (~/(?i)date/)                     : LocalDate.now(),
        (~/(?i)time/)                     : LocalTime.now(),
        (~/(?i)/)                         : "占位符"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    int index = dir.toString().lastIndexOf("/src/main/java/")
    if (index != -1) {
        packageName = dir.toString().substring(index + 15).replaceAll("/", ".")
    }
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    def tableName = table.getName()



    def modelDir = dir.toString() + "/model/"
    def modelFile = new File(modelDir)
    modelFile.mkdirs()
    new File(modelDir, className + ".java").withPrintWriter { out -> model(out, className, fields) }



    def mapperDir = dir.toString() + "/mapper/"
    def baseMapperDir = dir.toString() + "/mapper/base/"
    def baseMapperFile = new File(baseMapperDir)
    baseMapperFile.mkdirs()
    new File(baseMapperDir, className + "BaseMapper.java").withPrintWriter { out -> baseMapper(out, className, fields) }
    def mapperFile = new File(mapperDir, className + "Mapper.java")
    if (!mapperFile.exists()) {
        mapperFile.withPrintWriter { out -> mapper(out, className, fields) }
    }



    def xmlDir = dir.toString().substring(0, index + 10) + "/resources/mapper/"
    def baseXmlDir = dir.toString().substring(0, index + 10) + "/resources/mapper/base/"
    def baseXmlFile = new File(baseXmlDir)
    baseXmlFile.mkdirs()
    new File(baseXmlDir, className + "BaseMapper.xml").withPrintWriter { out -> baseXml(out, tableName, className, fields) }
    def xmlFile = new File(xmlDir, className + "Mapper.xml")
    if (!xmlFile.exists()) {
        xmlFile.withPrintWriter { out -> xml(out, tableName, className, fields) }
    }
}

def model(out, className, fields) {
    out.println "package ${packageName}.model;"
    out.println ""
    out.println "import io.swagger.annotations.ApiModel;"
    out.println "import io.swagger.annotations.ApiModelProperty;"
    out.println "import lombok.*;"
    out.println ""
    out.println "import java.io.Serializable;"
    out.println "import java.time.*;"
    out.println ""
    out.println "@Data"
    out.println "@Builder"
    out.println "@NoArgsConstructor"
    out.println "@AllArgsConstructor"
    out.println "@ApiModel(value = \"$className\")"
    out.println "public class $className implements Serializable {"
    out.println ""
    out.println "  public static final long serialVersionUID = 1L;"
    out.println ""
    fields.each() {
        if (propertiesContainField(it.right, commonProperties)) {
            if (it.commoent != "") {
                out.println " /**"
                out.println "  * ${it.comment}【${it.dataType}】"
                out.println "  */"
            }
            if (it.commoent != "") {
                out.println "  @ApiModelProperty(value = \"${it.comment}\", dataType = \"${it.dataType}\", example = \"${it.example}\", hidden = true)"
            }
            if (it.annos != "") out.println "  ${it.annos}"
            out.println "  private ${it.type} ${it.name};"
            out.println ""
        } else {
            if (it.commoent != "") {
                out.println " /**"
                out.println "  * ${it.comment}【${it.dataType}】"
                out.println "  */"
            }
            if (it.commoent != "") {
                out.println "  @ApiModelProperty(value = \"${it.comment}\", dataType = \"${it.dataType}\", example = \"${it.example}\")"
            }
            if (it.annos != "") out.println "  ${it.annos}"
            out.println "  private ${it.type} ${it.name};"
            out.println ""
        }
    }
    out.println ""
    out.println "}"
}

def baseMapper(out, className, fields) {
    out.println "package ${packageName}.mapper.base;"
    out.println ""
    out.println "import ${packageName}.model.${className};"
    out.println "import org.apache.ibatis.annotations.Param;"
    out.println ""
    out.println "import java.util.List;"
    out.println "import java.util.Map;"
    out.println ""
    out.println "public interface ${className}BaseMapper {"
    out.println ""
    out.println "    ${className} selectByPrimaryKey(@Param(\"id\") Long id);"
    out.println ""
    out.println "    List<${className}> selectByQuery(Map<String, Object> param);"
    out.println ""
    out.println "    Integer deleteByPrimaryKey(@Param(\"id\") Long id);"
    out.println ""
    out.println "    Integer count(Map<String, Object> param);"
    out.println ""
    out.println "    Long insert(Map<String, Object> param);"
    out.println ""
    out.println "    Integer update(Map<String, Object> param);"
    out.println ""
    out.println "}"
}

def mapper(out, className, fields) {
    out.println "package ${packageName}.mapper;"
    out.println ""
    out.println "import ${packageName}.mapper.base.${className}BaseMapper;"
    out.println "import org.apache.ibatis.annotations.Mapper;"
    out.println ""
    out.println "@Mapper"
    out.println "public interface ${className}Mapper extends ${className}BaseMapper {"
    out.println ""
    out.println "}"
}

def baseXml(out, tableName, className, fields) {
    int index = 0
    out.println "<?xml version='1.0' encoding='UTF-8' ?>"
    out.println "<!DOCTYPE mapper PUBLIC '-//mybatis.org//DTD Mapper 3.0//EN' 'http://mybatis.org/dtd/mybatis-3-mapper.dtd' >"
    out.println "<mapper namespace='${packageName}.mapper.base.${className}BaseMapper'>"
    out.println ""
    out.println "    <sql id='Base_Column_List' >"
    out.print "    "
    fields.each() {
        if (index != 0) {
            out.print ", "
        }
        out.print "${it.right}"
        index++
    }
    out.println ""
    out.println "    </sql>"
    out.println ""
    out.println "    <select id='selectByPrimaryKey' resultType='${packageName}.model.${className}' parameterType='java.lang.Long'>"
    out.println "        select "
    out.println "        <include refid='Base_Column_List' />"
    out.println "        from ${tableName} "
    out.println "        where id = #{id}"
    out.println "    </select>"
    out.println ""
    out.println "    <select id='selectByQuery' resultType='${packageName}.model.${className}' parameterType='java.util.Map'>"
    out.println "        select "
    out.println "        <include refid='Base_Column_List' />"
    out.println "        from ${tableName} "
    out.println "        <where>"
    out.println "            <include refid='query_filter'/>"
    out.println "        </where>"
    out.println "        <if test='start != null'>"
    out.println "            LIMIT #{start},#{limit}"
    out.println "        </if>"
    out.println "    </select>"
    out.println ""
    if (propertiesContainField(isDeleteProperties, fields)) {
        out.println "    <delete id='deleteByPrimaryKey' parameterType='java.lang.Long'>"
        out.println "        delete from ${tableName} where id = #{id}"
        out.println "    </delete>"
        out.println ""
    } else {
        out.println "    <delete id='deleteByPrimaryKey' parameterType='java.lang.Long'>"
        out.println "        update ${tableName} set ${isDeleteProperties[0]} = 1 where id = #{id}"
        out.println "    </delete>"
        out.println ""
    }
    out.println "    <select id='count' resultType='java.lang.Integer' parameterType='java.util.Map'>"
    out.println "        select count(*) from ${tableName}"
    out.println "        <where>"
    out.println "            <include refid='query_filter'/>"
    out.println "        </where>"
    out.println "    </select>"
    out.println ""
    out.println "    <insert id='insert' parameterType='java.util.Map' useGeneratedKeys='true' keyProperty='id'>"
    out.println "        insert into ${tableName}"
    out.println "        <trim prefix='(' suffix=')' suffixOverrides=','>"
    fields.each() {
        if (propertiesContainField(it.right, gmtCreate)) {
            out.println "            ${it.right},"
        } else if (propertiesContainField(it.right, gmtModified)) {
            out.println "            ${it.right},"
        } else if (propertiesContainField(it.right, isDeleteProperties)){
            out.println "            ${it.right},"
        } else {
            out.println "            <if test='${it.left} != null'>${it.right},</if>"
        }
    }
    out.println "        </trim>"
    out.println "        <trim prefix='values (' suffix=')' suffixOverrides=','>"
    fields.each() {
        if (propertiesContainField(it.right, gmtCreate)) {
            out.println "            now(),"
        } else if (propertiesContainField(it.right, gmtModified)) {
            out.println "            now(),"
        } else if (propertiesContainField(it.right, isDeleteProperties)){
            out.println "            0,"
        } else {
            out.println "            <if test='${it.left} != null'>#{${it.left}},</if>"
        }
    }
    out.println "        </trim>"
    out.println "    </insert>"
    out.println ""
    out.println "    <update id='update' parameterType='java.util.Map'>"
    out.println "        update ${tableName}"
    out.println "        <set>"
    fields.each() {
        if (propertiesContainField(it.right, gmtModified)) {
            out.println "            <if test='${it.left}'>${it.right} = now(),</if>"
        } else {
            out.println "            <if test='${it.left}'>${it.right} = #{${it.left}},</if>"
        }
    }
    out.println "        </set>"
    out.println "        where id = #{id}"
    out.println "    </update>"
    out.println ""
    out.println "    <sql id='query_filter'>"
    fields.each() {
        if (propertiesContainField(it.right, isDeleteProperties)) {
            out.println "        and ${it.right} != 1"
        } else {
            out.println "        <if test='${it.left} != null'>and ${it.right} = #{${it.left}}</if>"
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

boolean fieldsContainPropertie(propertie, fields) {
    def isExsit = false
    fields.each() {
        if (propertie == it.right) {
            isExsit = true
        }
    }
    isExsit
}

boolean propertiesContainField(field, properties) {
    def isExsit = false
    properties.each() {
        if (field == it) {
            isExsit = true
        }
    }
    isExsit
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        def dataType = dataTypeMapping.find { p, t -> p.matcher(spec).find() }.value
        def example = exampleMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           left    : javaName(col.getName(), false),
                           right   : col.getName(),
                           name    : javaName(col.getName(), false),
                           dataType: dataType,
                           example : example,
                           type    : typeStr,
                           comment : col.getComment(),
                           annos   : ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

