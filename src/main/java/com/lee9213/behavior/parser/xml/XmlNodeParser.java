package com.lee9213.behavior.parser.xml;

import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.parser.condition.ConditionalOnClass;
import org.springframework.context.annotation.Conditional;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 15:07
 */
@ConditionalOnClass(XmlNodeParser.class)
public class XmlNodeParser {

    public static BehaviorNodeWrapper parse(String xml) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
