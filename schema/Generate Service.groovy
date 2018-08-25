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
    def tableComment = table.getComment()
    def paramName = javaName(table.getName(), false)
    def fields = calcFields(table)
    def serviceDir = dir.toString() + "/service/"
    def serviceImplDir = dir.toString() + "/service/impl/"
    def serviceImplFile = new File(serviceImplDir)
    serviceImplFile.mkdirs()
    new File(serviceImplDir, className + "ServiceImpl.java").withPrintWriter { out -> serviceImpl(out, className, paramName, fields) }
    new File(serviceDir, className + "Service.java").withPrintWriter { out -> service(out, className, tableComment, paramName, fields) }
}

def serviceImpl(out, className, paramName, fields) {
    out.println "package ${packageName}.service.impl;"
    out.println ""
    out.println "import ${basePackageName}.core.common.Result;"
    out.println "import ${basePackageName}.core.utils.Param2;"
    out.println "import ${packageName}.mapper.${className}Mapper;"
    out.println "import ${packageName}.model.${className};"
    out.println "import ${packageName}.service.${className}Service;"
    out.println "import com.github.pagehelper.Page;"
    out.println "import com.github.pagehelper.PageHelper;"
    out.println "import org.springframework.beans.factory.annotation.Autowired;"
    out.println "import org.springframework.http.HttpStatus;"
    out.println "import org.springframework.http.ResponseEntity;"
    out.println "import org.springframework.stereotype.Service;"
    out.println ""
    out.println "import java.util.List;"
    out.println ""
    out.println "@Service"
    out.println "public class ${className}ServiceImpl implements ${className}Service {"
    out.println ""
    out.println "    @Autowired"
    out.println "    @SuppressWarnings(\"all\")"
    out.println "    private ${className}Mapper ${paramName}Mapper;"
    out.println ""
    out.println ""
    out.println "    @Override"
    out.println "    public ResponseEntity insert(${className} ${paramName}) {"
    out.println "        if (${paramName} == null) {"
    out.println "            return ResponseEntity.ok(Result.success(HttpStatus.BAD_REQUEST));"
    out.println "        }"
    out.println "        ${paramName}Mapper.insert(Param2.toMap(${paramName}));"
    out.println "        return ResponseEntity.ok(Result.success(HttpStatus.CREATED));"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public ResponseEntity deleteByPrimaryKey(Long id) {"
    out.println "        if (id == null || id < 1L) {"
    out.println "            return ResponseEntity.ok(Result.success(HttpStatus.BAD_REQUEST));"
    out.println "        }"
    out.println "        Integer affectedRows = ${paramName}Mapper.deleteByPrimaryKey(id);"
    out.println "        if (affectedRows < 1) {"
    out.println "            return ResponseEntity.ok(Result.success(HttpStatus.NOT_FOUND));"
    out.println "        }"
    out.println "        return ResponseEntity.ok(Result.success(HttpStatus.NO_CONTENT));"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public ResponseEntity update(${className} ${paramName}) {"
    out.println "        if (${paramName} == null) {"
    out.println "            return ResponseEntity.ok(Result.success(HttpStatus.BAD_REQUEST));"
    out.println "        }"
    out.println "        Integer affectedRows = ${paramName}Mapper.update(Param2.toMap(${paramName}));"
    out.println "        if (affectedRows < 1) {"
    out.println "            return ResponseEntity.ok(Result.success(HttpStatus.NOT_FOUND));"
    out.println "        }"
    out.println "        return ResponseEntity.ok(Result.success(HttpStatus.RESET_CONTENT));"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public ResponseEntity selectByPrimaryKey(Long id) {"
    out.println "        if (id == null || id < 1L) {"
    out.println "            return ResponseEntity.ok(Result.success(HttpStatus.BAD_REQUEST));"
    out.println "        }"
    out.println "        ${className} ${paramName} = ${paramName}Mapper.selectByPrimaryKey(id);"
    out.println "        if(${paramName} == null){"
    out.println "            return ResponseEntity.ok(Result.success(HttpStatus.NOT_FOUND));"
    out.println "        }"
    out.println "        Result result = Result.success(HttpStatus.OK);"
    out.println "        result.setEntry(${paramName});"
    out.println "        return ResponseEntity.ok(result);"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public ResponseEntity selectByQuery(Integer pageNum, Integer pageSize, ${className} ${paramName}) {"
    out.println "        if (${paramName} == null || pageNum < 1 || pageSize < 1) {"
    out.println "            return ResponseEntity.ok().body(Result.success(HttpStatus.BAD_REQUEST));"
    out.println "        }"
    out.println "        PageHelper.startPage(pageNum, pageSize);"
    out.println "        List<${className}> ${paramName}List = ${paramName}Mapper.selectByQuery(Param2.toNotContainBlankMap(${paramName}));"
    out.println "        Long total = ((Page) ${paramName}List).getTotal();"
    out.println "        Result result = Result.success(HttpStatus.OK);"
    out.println "        result.setEntry(${paramName}List);"
    out.println "        result.setTotal(total);"
    out.println "        return ResponseEntity.ok(result);"
    out.println "    }"
    out.println ""
    out.println "}"
}

def service(out, className, tableComment, paramName, fields) {
    out.println "package ${packageName}.service;"
    out.println ""
    out.println "import ${packageName}.model.${className};"
    out.println "import org.springframework.http.ResponseEntity;"
    out.println ""
    out.println "public interface ${className}Service {"
    out.println ""
    out.println "    /**"
    out.println "     * 新增${tableComment}"
    out.println "     *"
    out.println "     * @param ${paramName}"
    out.println "     * @return"
    out.println "     */"
    out.println "    ResponseEntity insert(${className} ${paramName});"
    out.println ""
    out.println "    /**"
    out.println "     * 通过主键删除"
    out.println "     *"
    out.println "     * @param id"
    out.println "     * @return"
    out.println "     */"
    out.println "    ResponseEntity deleteByPrimaryKey(Long id);"
    out.println ""
    out.println "    /**"
    out.println "     * 更新${tableComment}"
    out.println "     *"
    out.println "     * @param ${paramName}"
    out.println "     * @return"
    out.println "     */"
    out.println "    ResponseEntity update(${className} ${paramName});"
    out.println ""
    out.println "    /**"
    out.println "     * 通过主键查询"
    out.println "     *"
    out.println "     * @param id"
    out.println "     * @return"
    out.println "     */"
    out.println "    ResponseEntity selectByPrimaryKey(Long id);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询"
    out.println "     *"
    out.println "     * @param pageNum"
    out.println "     * @param pageSize"
    out.println "     * @param ${paramName}"
    out.println "     * @return"
    out.println "     */"
    out.println "    ResponseEntity selectByQuery(Integer pageNum, Integer pageSize, ${className} ${paramName});"
    out.println ""
    out.println "}"
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
