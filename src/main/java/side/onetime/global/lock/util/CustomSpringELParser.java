package side.onetime.global.lock.util;

import lombok.RequiredArgsConstructor;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

@RequiredArgsConstructor
public class CustomSpringELParser {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public String getDynamicValue(Method method, Object[] args, String expression) {
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = nameDiscoverer.getParameterNames(method);

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        return parser.parseExpression(expression).getValue(context, String.class);
    }
}
