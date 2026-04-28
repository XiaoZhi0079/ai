package com.example.ai.service.impl;

import com.example.ai.Factory.ChatClientFactory;
import com.example.ai.config.ChatModelProperties;
import com.example.ai.mapper.AiSqlQueryMapper;
import com.example.ai.pojo.AiSqlQueryResponse;
import com.example.ai.security.Role;
import com.example.ai.service.AiSqlQueryService;
import com.example.ai.service.OperationLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeKeyExpression;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 自然语言数据查询服务。
 *
 * 这一版按照“前端列表页可见内容”来设计权限：
 * - 学生使用 ai_student_* 视图体系；
 * - 教师使用 ai_teacher_* 视图体系；
 * - 管理员不走视图，直接使用真实业务表白名单；
 * - 所有角色都只允许 SELECT，只允许受控字段，只允许有限结果集。
 *
 * 也就是说，学生和教师在 AI 查询里能接触到的 schema，
 * 与他们在前端列表页实际看到的字段集合保持一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSqlQueryServiceImpl implements AiSqlQueryService {

    private static final int MAX_ROWS = 100;
    private static final Set<String> ALLOWED_FUNCTIONS = Set.of("COUNT", "AVG", "SUM", "MIN", "MAX");
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    private final ChatClientFactory chatClientFactory;
    private final ChatModelProperties chatModelProperties;
    private final AiSqlQueryMapper aiSqlQueryMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Override
    public AiSqlQueryResponse query(String question, String modelName, Integer userId, String roleText) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("Question is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Missing user");
        }

        Role role = resolveRole(roleText);
        SourceBundle sourceBundle = sourceBundleFor(role);
        String sql = generateSql(question.trim(), resolveModel(modelName), sourceBundle, role);
        String safeSql = validateAndRewrite(sql, sourceBundle);
        List<Map<String, Object>> rows = aiSqlQueryMapper.executeSelect(safeSql);

        operationLogService.log(role.name() + "#" + userId, "执行 AI 数据查询: " + question.trim());
        return new AiSqlQueryResponse(safeSql, rows == null ? 0 : rows.size(), rows == null ? List.of() : rows);
    }

    /**
     * 将请求头里的角色字符串转换成系统角色枚举。
     */
    private Role resolveRole(String roleText) {
        try {
            return Role.valueOf(StringUtils.hasText(roleText) ? roleText.trim().toUpperCase(Locale.ROOT) : "");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported role");
        }
    }

    /**
     * 没指定模型时，回退到配置里第一个可用模型。
     */
    private String resolveModel(String requestedModel) {
        if (StringUtils.hasText(requestedModel)) {
            return requestedModel.trim();
        }
        if (chatModelProperties.getPlatforms() == null) {
            throw new IllegalArgumentException("No chat model configured");
        }
        return chatModelProperties.getPlatforms().stream()
                .filter(platform -> platform.getOptions() != null && !platform.getOptions().isEmpty())
                .map(platform -> platform.getOptions().get(0).getModel())
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No chat model configured"));
    }

    /**
     * 给模型的 schema 只包含当前角色允许看到的查询源。
     *
     * - 学生：只能看到 ai_student_* 列表视图；
     * - 教师：只能看到 ai_teacher_* 列表视图；
     * - 管理员：直接看到真实业务表白名单。
     */
    private String generateSql(String question, String modelName, SourceBundle sourceBundle, Role role) {
        String systemPrompt = """
                你是一个严格的 MySQL 查询生成器。
                你的任务是根据用户问题，基于给定查询源和字段说明，生成一条 MySQL SELECT 语句。
                必须遵守以下规则：
                1. 只能输出 JSON，不要输出 Markdown，不要输出解释。
                2. JSON 结构必须是 {"sql":"SELECT ..."}。
                3. 只能生成单条 SELECT 查询。
                4. 只能访问给定查询源和允许字段。
                5. 允许多表 JOIN，但只能 JOIN 当前角色允许的查询源。
                6. 禁止 INSERT、UPDATE、DELETE、DROP、ALTER、TRUNCATE、CREATE、REPLACE、CALL、UNION、子查询、注释。
                7. 禁止 SELECT *。
                8. 如果无法安全表达用户问题，返回 {"sql":""}。
                """;

        String userPrompt = """
                当前用户角色：%s

                当前可用查询源与字段说明：
                %s

                用户问题：%s
                """.formatted(role.name(), sourceBundle.schemaDescription(), question);

        String content = chatClientFactory.getClient(modelName)
                .prompt(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))))
                .call()
                .content();

        return extractSql(content);
    }

    /**
     * 兼容模型返回 JSON 或裸 SQL 的情况。
     */
    private String extractSql(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("AI did not return SQL");
        }

        String trimmed = content.trim();
        String jsonCandidate = trimmed;
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(trimmed);
        if (!trimmed.startsWith("{") && matcher.find()) {
            jsonCandidate = matcher.group();
        }

        try {
            JsonNode root = objectMapper.readTree(jsonCandidate);
            String sql = root.path("sql").asText("").trim();
            if (!StringUtils.hasText(sql)) {
                throw new IllegalArgumentException("AI could not build a safe query");
            }
            return stripTrailingSemicolon(sql);
        } catch (Exception ex) {
            String directSql = stripTrailingSemicolon(trimmed.replace("```sql", "").replace("```", "").trim());
            if (!StringUtils.hasText(directSql)) {
                throw new IllegalArgumentException("Invalid AI SQL response", ex);
            }
            return directSql;
        }
    }

    /**
     * 对 AI 返回的 SQL 做结构级安全校验。
     */
    private String validateAndRewrite(String sql, SourceBundle sourceBundle) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select)) {
                throw new IllegalArgumentException("Only SELECT statements are allowed");
            }
            if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
                throw new IllegalArgumentException("Only simple SELECT queries are allowed");
            }
            if (plainSelect.getIntoTables() != null && !plainSelect.getIntoTables().isEmpty()) {
                throw new IllegalArgumentException("SELECT INTO is not allowed");
            }
            if (plainSelect.getFromItem() == null) {
                throw new IllegalArgumentException("Missing FROM clause");
            }

            AliasContext aliasContext = new AliasContext();
            registerFromItem(plainSelect.getFromItem(), aliasContext, sourceBundle);
            validateJoins(plainSelect.getJoins(), aliasContext, sourceBundle);
            validateSelectItems(plainSelect.getSelectItems(), aliasContext, sourceBundle);
            validateExpression(plainSelect.getWhere(), aliasContext, sourceBundle);
            validateGroupBy(plainSelect.getGroupBy(), aliasContext, sourceBundle);
            validateExpression(plainSelect.getHaving(), aliasContext, sourceBundle);
            validateOrderBy(plainSelect.getOrderByElements(), aliasContext, sourceBundle);
            enforceLimit(plainSelect);

            return stripTrailingSemicolon(plainSelect.toString());
        } catch (JSQLParserException ex) {
            throw new IllegalArgumentException("Invalid SQL generated by AI", ex);
        }
    }

    /**
     * 注册主查询源。
     */
    private void registerFromItem(FromItem fromItem, AliasContext aliasContext, SourceBundle sourceBundle) {
        if (!(fromItem instanceof Table table)) {
            throw new IllegalArgumentException("Subqueries are not allowed");
        }

        String sourceName = normalizeName(table.getName());
        SourceRule rule = sourceBundle.rule(sourceName)
                .orElseThrow(() -> new IllegalArgumentException("Only allowed role views may be queried"));
        String alias = resolveAlias(table.getAlias(), sourceName);
        aliasContext.register(alias, rule);
    }

    /**
     * 学生和教师支持在本角色视图体系内联查；管理员支持在白名单真实表内联查。
     */
    private void validateJoins(List<Join> joins, AliasContext aliasContext, SourceBundle sourceBundle) {
        if (joins == null) {
            return;
        }
        for (Join join : joins) {
            if (join.isRight() || join.isFull() || join.isCross() || join.isNatural() || join.isApply() || join.isSemi()) {
                throw new IllegalArgumentException("Join type not allowed");
            }
            registerFromItem(join.getRightItem(), aliasContext, sourceBundle);
            validateExpression(join.getOnExpression(), aliasContext, sourceBundle);
            if (join.getUsingColumns() != null) {
                for (Column column : join.getUsingColumns()) {
                    validateColumn(column, aliasContext, sourceBundle);
                }
            }
        }
    }

    private void validateSelectItems(List<SelectItem> selectItems, AliasContext aliasContext, SourceBundle sourceBundle) {
        if (selectItems == null || selectItems.isEmpty()) {
            throw new IllegalArgumentException("No selected fields");
        }
        for (SelectItem selectItem : selectItems) {
            if (selectItem instanceof AllColumns || selectItem instanceof AllTableColumns) {
                throw new IllegalArgumentException("SELECT * is not allowed");
            }
            if (selectItem instanceof SelectExpressionItem expressionItem) {
                validateExpression(expressionItem.getExpression(), aliasContext, sourceBundle);
                continue;
            }
            throw new IllegalArgumentException("Unsupported select item");
        }
    }

    private void validateGroupBy(GroupByElement groupBy, AliasContext aliasContext, SourceBundle sourceBundle) {
        if (groupBy == null || groupBy.getGroupByExpressions() == null) {
            return;
        }
        for (Expression expression : groupBy.getGroupByExpressions()) {
            validateExpression(expression, aliasContext, sourceBundle);
        }
    }

    private void validateOrderBy(List<OrderByElement> orderByElements, AliasContext aliasContext, SourceBundle sourceBundle) {
        if (orderByElements == null) {
            return;
        }
        for (OrderByElement element : orderByElements) {
            validateExpression(element.getExpression(), aliasContext, sourceBundle);
        }
    }

    /**
     * 递归校验表达式树，确保出现的每个列和函数都在允许范围内。
     */
    private void validateExpression(Expression expression, AliasContext aliasContext, SourceBundle sourceBundle) {
        if (expression == null) {
            return;
        }
        if (expression instanceof Column column) {
            validateColumn(column, aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof Function function) {
            validateFunction(function, aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof BinaryExpression binaryExpression) {
            validateExpression(binaryExpression.getLeftExpression(), aliasContext, sourceBundle);
            validateExpression(binaryExpression.getRightExpression(), aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof Parenthesis parenthesis) {
            validateExpression(parenthesis.getExpression(), aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof SignedExpression signedExpression) {
            validateExpression(signedExpression.getExpression(), aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof CastExpression castExpression) {
            validateExpression(castExpression.getLeftExpression(), aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof NotExpression notExpression) {
            validateExpression(notExpression.getExpression(), aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof IsNullExpression isNullExpression) {
            validateExpression(isNullExpression.getLeftExpression(), aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof Between between) {
            validateExpression(between.getLeftExpression(), aliasContext, sourceBundle);
            validateExpression(between.getBetweenExpressionStart(), aliasContext, sourceBundle);
            validateExpression(between.getBetweenExpressionEnd(), aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof InExpression inExpression) {
            validateExpression(inExpression.getLeftExpression(), aliasContext, sourceBundle);
            if (inExpression.getRightItemsList() instanceof ExpressionList expressionList) {
                for (Expression item : expressionList.getExpressions()) {
                    validateExpression(item, aliasContext, sourceBundle);
                }
                return;
            }
            throw new IllegalArgumentException("Subqueries are not allowed");
        }
        if (expression instanceof CaseExpression caseExpression) {
            validateExpression(caseExpression.getSwitchExpression(), aliasContext, sourceBundle);
            if (caseExpression.getWhenClauses() != null) {
                for (WhenClause whenClause : caseExpression.getWhenClauses()) {
                    validateExpression(whenClause.getWhenExpression(), aliasContext, sourceBundle);
                    validateExpression(whenClause.getThenExpression(), aliasContext, sourceBundle);
                }
            }
            validateExpression(caseExpression.getElseExpression(), aliasContext, sourceBundle);
            return;
        }
        if (expression instanceof NullValue || expression instanceof LongValue || expression instanceof StringValue
                || expression instanceof DateTimeLiteralExpression || expression instanceof TimeKeyExpression) {
            return;
        }
    }

    /**
     * 聚合函数白名单。
     */
    private void validateFunction(Function function, AliasContext aliasContext, SourceBundle sourceBundle) {
        String functionName = function.getName() == null ? "" : function.getName().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_FUNCTIONS.contains(functionName)) {
            throw new IllegalArgumentException("Function not allowed: " + functionName);
        }
        if (function.isAllColumns()) {
            if (!"COUNT".equals(functionName)) {
                throw new IllegalArgumentException("Only COUNT(*) is allowed");
            }
            return;
        }
        if (function.getParameters() == null || function.getParameters().getExpressions() == null) {
            return;
        }
        for (Expression expression : function.getParameters().getExpressions()) {
            validateExpression(expression, aliasContext, sourceBundle);
        }
    }

    /**
     * 列必须属于某个已注册查询源，并且字段名位于该查询源的白名单中。
     */
    private void validateColumn(Column column, AliasContext aliasContext, SourceBundle sourceBundle) {
        String columnName = normalizeName(column.getColumnName());
        String alias = normalizeName(column.getTable() == null ? null : column.getTable().getName());
        if (!StringUtils.hasText(alias)) {
            if (aliasContext.size() != 1) {
                throw new IllegalArgumentException("Columns must use table aliases when querying multiple sources");
            }
            alias = aliasContext.singleAlias();
            column.setTable(new Table(alias));
        }

        String finalAlias = alias;
        SourceRule rule = aliasContext.rule(finalAlias);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown alias: " + finalAlias);
        }
        if (!rule.allowedColumns().contains(columnName)) {
            throw new IllegalArgumentException("Column not allowed: " + finalAlias + "." + columnName);
        }
    }

    /**
     * 所有 AI 查询都自动限制返回行数。
     */
    private void enforceLimit(PlainSelect plainSelect) {
        Limit currentLimit = plainSelect.getLimit();
        if (currentLimit == null) {
            plainSelect.setLimit(new Limit().withRowCount(new LongValue(MAX_ROWS)));
            return;
        }
        Long rowCount = parseLongValue(currentLimit.getRowCount());
        if (rowCount == null) {
            throw new IllegalArgumentException("LIMIT must be a numeric literal");
        }
        if (rowCount > MAX_ROWS) {
            throw new IllegalArgumentException("LIMIT exceeds maximum allowed rows");
        }
    }

    private Long parseLongValue(Expression expression) {
        if (expression instanceof LongValue longValue) {
            return longValue.getValue();
        }
        return null;
    }

    /**
     * 学生/教师使用角色专属视图体系；管理员直接使用真实业务表白名单。
     */
    private SourceBundle sourceBundleFor(Role role) {
        return switch (role) {
            case STUDENT -> new SourceBundle(Map.of(
                    "ai_student_teacher_list_view", new SourceRule(
                            "ai_student_teacher_list_view",
                            "学生端教师信息列表视图。对应教师信息页列表字段。",
                            Set.of("id", "name", "gender", "phone", "department", "title", "research_field")
                    ),
                    "ai_student_student_list_view", new SourceRule(
                            "ai_student_student_list_view",
                            "学生端学生信息列表视图。对应学生信息页列表字段。",
                            Set.of("id", "name", "gender", "grade", "major", "class_name")
                    ),
                    "ai_student_course_list_view", new SourceRule(
                            "ai_student_course_list_view",
                            "学生端课程信息列表视图。对应课程信息页列表字段。",
                            Set.of("id", "course_name", "teacher_name", "credit", "schedule", "begin_date", "end_date")
                    ),
                    "ai_student_grade_list_view", new SourceRule(
                            "ai_student_grade_list_view",
                            "学生端成绩信息列表视图。对应成绩信息页列表字段。",
                            Set.of("id", "student_name", "course_name", "score", "semester")
                    )
            ));
            case TEACHER -> new SourceBundle(Map.of(
                    "ai_teacher_teacher_list_view", new SourceRule(
                            "ai_teacher_teacher_list_view",
                            "教师端教师信息列表视图。对应教师信息页列表字段。",
                            Set.of("id", "name", "gender", "phone", "department", "title", "research_field")
                    ),
                    "ai_teacher_student_list_view", new SourceRule(
                            "ai_teacher_student_list_view",
                            "教师端学生信息列表视图。对应学生信息页列表字段。",
                            Set.of("id", "name", "gender", "grade", "major", "class_name", "dormitory", "guardian_phone")
                    ),
                    "ai_teacher_course_list_view", new SourceRule(
                            "ai_teacher_course_list_view",
                            "教师端课程信息列表视图。对应课程信息页列表字段。",
                            Set.of("id", "course_name", "teacher_name", "credit", "schedule", "begin_date", "end_date")
                    ),
                    "ai_teacher_grade_list_view", new SourceRule(
                            "ai_teacher_grade_list_view",
                            "教师端成绩信息列表视图。对应成绩信息页列表字段。",
                            Set.of("id", "student_name", "course_name", "score", "semester")
                    )
            ));
            case ADMIN -> new SourceBundle(Map.of(
                    "teachers", new SourceRule(
                            "teachers",
                            "教师真实业务表。管理员可直接查询。",
                            Set.of("id", "user_id", "name", "gender", "phone", "department", "title", "research_field", "office_address", "created_at", "updated_at")
                    ),
                    "students", new SourceRule(
                            "students",
                            "学生真实业务表。管理员可直接查询。",
                            Set.of("id", "user_id", "name", "gender", "grade", "major", "class_name", "dormitory", "guardian_phone", "created_at", "updated_at")
                    ),
                    "courses", new SourceRule(
                            "courses",
                            "课程真实业务表。管理员可直接查询。",
                            Set.of("id", "course_name", "teacher_id", "credit", "begin_date", "end_date", "schedule", "description", "created_at", "updated_at")
                    ),
                    "grades", new SourceRule(
                            "grades",
                            "成绩真实业务表。管理员可直接查询。",
                            Set.of("id", "student_id", "course_id", "score", "semester", "created_at", "updated_at")
                    )
            ));
        };
    }

    private String stripTrailingSemicolon(String sql) {
        String trimmed = sql == null ? "" : sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveAlias(Alias alias, String defaultAlias) {
        return normalizeName(alias == null ? defaultAlias : alias.getName());
    }

    /**
     * 单个查询源定义。
     */
    private record SourceRule(String sourceName, String description, Set<String> allowedColumns) {
        String schemaLine() {
            return "%s: %s 可用字段：%s".formatted(sourceName, description, String.join(", ", allowedColumns));
        }
    }

    /**
     * 当前角色可用查询源集合。
     */
    private record SourceBundle(Map<String, SourceRule> rules) {
        Optional<SourceRule> rule(String sourceName) {
            return Optional.ofNullable(rules.get(sourceName));
        }

        String schemaDescription() {
            return rules.values().stream().map(SourceRule::schemaLine).reduce((left, right) -> left + "\n" + right).orElse("");
        }
    }

    /**
     * 查询里的 alias -> 查询源定义。
     */
    private static final class AliasContext {
        private final Map<String, SourceRule> aliasToRule = new LinkedHashMap<>();

        void register(String alias, SourceRule rule) {
            if (aliasToRule.containsKey(alias)) {
                throw new IllegalArgumentException("Duplicate alias: " + alias);
            }
            aliasToRule.put(alias, rule);
        }

        SourceRule rule(String alias) {
            return aliasToRule.get(alias);
        }

        int size() {
            return aliasToRule.size();
        }

        String singleAlias() {
            return aliasToRule.keySet().iterator().next();
        }
    }
}
