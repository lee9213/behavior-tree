package com.lee9213.behavior;

import com.lee9213.behavior.parser.json.JsonNodeParser;
import com.lee9213.behavior.parser.spring.EnableBehavior;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 15:09
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class BehaviorTreeTest {

    @Test
    public void execute() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(getClass().getResource("/").getPath() + "test.json");
        FileChannel channel = fileInputStream.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)channel.size());
        channel.read(byteBuffer);
        String json = new String(byteBuffer.array(), "UTF-8");
        channel.close();
        fileInputStream.close();

        TestContext testContext = new TestContext();
        BehaviorTree behaviorTree = new BehaviorTree<>(new JsonNodeParser().parse(json, TestNodeResult.class));
        behaviorTree.execute(testContext);
    }
}

@Configuration
@EnableBehavior
@ComponentScan("com.lee9213.behavior")
class TestConfiguration {

}

