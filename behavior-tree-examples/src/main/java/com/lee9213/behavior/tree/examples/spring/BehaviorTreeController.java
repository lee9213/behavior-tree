package com.lee9213.behavior.tree.examples.spring;

import com.lee9213.behavior.tree.NodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行为树控制器
 */
@RestController
public class BehaviorTreeController {

    @Autowired
    private BehaviorTreeService behaviorTreeService;

    @GetMapping("/execute")
    public String execute() {
        NodeResult result = behaviorTreeService.execute();
        return "行为树执行结果: " + result;
    }

    @GetMapping("/execute-manual")
    public String executeManual() {
        NodeResult result = behaviorTreeService.executeManualTree();
        return "手动创建的行为树执行结果: " + result;
    }
}
