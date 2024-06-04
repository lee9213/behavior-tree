package com.lee9213.behavior.parser.spring;

import com.lee9213.behavior.node.IActionNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 17:41
 */
public class SpringNodeUtil {
    private static ApplicationContext applicationContext;
    private static Map<String, IActionNode> actionNodeMap;

    private SpringNodeUtil() {

    }

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static IActionNode getBehaviorNode(String beanName) {
        if (actionNodeMap == null) {
            actionNodeMap = applicationContext.getBeansOfType(IActionNode.class);
        }
        return actionNodeMap.get(beanName);
    }
}
