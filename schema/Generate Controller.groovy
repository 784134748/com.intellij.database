import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = ""
basePackageName = ""
typeMapping = [
        (~/(?i)bigint/)                   : "Long",
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    int index = dir.toString().lastIndexOf("/src/main/java/")
    if (index != -1) {
        packageName = dir.toString().substring(index + 15).replaceAll("/", ".")
    }
    index = packageName.lastIndexOf(".")
    if (index != -1) {
        basePackageName = packageName.toString().substring(0, index)
    }
    def className = javaName(table.getName(), true)
    def paramName = javaName(table.getName(), false)
    def tableComment = table.getComment()
    def fields = calcFields(table)
    def controllerDir = dir.toString() + "/controller/"
    def controllerlFile = new File(controllerDir)
    controllerlFile.mkdirs()
    new File(controllerDir, className + "Controller.java").withPrintWriter { out -> controller(out, className, tableComment, paramName, fields) }
}

def controller(out, className, tableComment, paramName, fields) {
    out.println"package ${packageName}.controller;"
    out.println""
    out.println"import ${packageName}.service.${className}Service;"
    out.println"import ${packageName}.model.${className};"
    out.println"import io.swagger.annotations.Api;"
    out.println"import io.swagger.annotations.ApiParam;"
    out.println"import io.swagger.annotations.ApiOperation;"
    out.println"import org.springframework.beans.factory.annotation.Autowired;"
    out.println"import org.springframework.http.ResponseEntity;"
    out.println"import org.springframework.web.bind.annotation.*;"
    out.println""
    out.println"@RestController"
    out.println"@Api(tags = \"${tableComment}相关接口\")"
    out.println"@RequestMapping(value = \"/v1\")"
    out.println"public class ${className}Controller {"
    out.println""
    out.println"    @Autowired"
    out.println"    private ${className}Service ${paramName}Service;"
    out.println""
    out.println"    @ApiOperation(value = \"新增${tableComment}\")"
    out.println"    @PostMapping(\"/${paramName}\")"
    out.println"    public ResponseEntity<?> save${className}(@RequestBody ${className} ${paramName}) {"
    out.println"        return ${paramName}Service.insert(${paramName});"
    out.println"    }"
    out.println""
    out.println"    @ApiOperation(value = \"删除${tableComment}\")"
    out.println"    @DeleteMapping(\"/${paramName}/{${paramName}_id}\")"
    out.println"    public ResponseEntity<?> delete${className}ById(@PathVariable(\"${paramName}_id\") Long ${paramName}Id) {"
    out.println"        return ${paramName}Service.deleteByPrimaryKey(${paramName}Id);"
    out.println"    }"
    out.println""
    out.println"    @ApiOperation(value = \"更新${tableComment}\")"
    out.println"    @PutMapping(\"/${paramName}/{${paramName}_id}\")"
    out.println"    public ResponseEntity<?> update${className}ById(@PathVariable(\"${paramName}_id\") Long ${paramName}Id, @RequestBody ${className} ${paramName}) {"
    out.println"        ${paramName}.setId(${paramName}Id);"
    out.println"        return ${paramName}Service.update(${paramName});"
    out.println"    }"
    out.println""
    out.println"    @ApiOperation(value = \"分页查询${tableComment}\")"
    out.println"    @GetMapping(\"/${paramName}/list\")"
    out.println"    public ResponseEntity<?> get${className}List(@ApiParam(name = \"pageNum\", value = \"当前页码\", required = true) @RequestParam(name = \"pageNum\", defaultValue = \"1\") Integer pageNum,"
    out.println"                                                @ApiParam(name = \"pageSize\", value = \"页大小\", required = true) @RequestParam(name = \"pageSize\", defaultValue = \"10\") Integer pageSize) {"
    out.println"        ${className} ${paramName} = ${className}.builder().build();"
    out.println"        return ${paramName}Service.selectByQuery(pageNum, pageSize, ${paramName});"
    out.println"    }"
    out.println""
    out.println"    @ApiOperation(value = \"${tableComment}详情\")"
    out.println"    @GetMapping(\"/${paramName}/{${paramName}_id}\")"
    out.println"    public ResponseEntity<?> get${className}ById(@PathVariable(\"${paramName}_id\") Long ${paramName}Id) {"
    out.println"        return ${paramName}Service.selectByPrimaryKey(${paramName}Id);"
    out.println"    }"
    out.println""
    out.println"}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           left    : javaName(col.getName(), false),
                           right   : col.getName(),
                           name    : javaName(col.getName(), false),
                           dataType: col.getDataType(),
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
