package com.erc20deploy.controller;

import com.erc20deploy.service.ContractDeployService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ContractController {

    private final ContractDeployService deployService;

    public ContractController(ContractDeployService deployService) {
        this.deployService = deployService;
    }

    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deploy() {
        try {
            String contractAddress = deployService.deployContract();
            return ResponseEntity.ok(Map.of("contractAddress", contractAddress));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
