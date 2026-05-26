package com.erc20deploy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

@Service
public class ContractDeployService {

    @Value("${besu.rpc.url:http://localhost:8545}")
    private String besuRpcUrl;

    @Value("${besu.deployer.private-key:0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63}")
    private String privateKey;

    @Value("${besu.chain.id:1337}")
    private long chainId;

    @Value("${besu.gas.limit:3000000}")
    private long gasLimit;

    @Value("${besu.gas.price:0}")
    private long gasPrice;

    private String bytecode;

    @PostConstruct
    public void loadBytecode() throws Exception {
        ClassPathResource resource = new ClassPathResource("SimpleERC20.bin");
        bytecode = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    }

    public String deployContract() throws Exception {
        Web3j web3j = Web3j.build(new HttpService(besuRpcUrl));
        try {
            Credentials credentials = Credentials.create(privateKey);

            BigInteger nonce = web3j
                .ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send()
                .getTransactionCount();

            RawTransaction rawTx = RawTransaction.createContractTransaction(
                nonce,
                BigInteger.valueOf(gasPrice),
                BigInteger.valueOf(gasLimit),
                BigInteger.ZERO,
                "0x" + bytecode
            );

            byte[] signed = TransactionEncoder.signMessage(rawTx, chainId, credentials);
            String hexValue = Numeric.toHexString(signed);

            EthSendTransaction ethSend = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSend.hasError()) {
                throw new RuntimeException("Transaction error: " + ethSend.getError().getMessage());
            }

            String txHash = ethSend.getTransactionHash();
            TransactionReceiptProcessor processor =
                new PollingTransactionReceiptProcessor(web3j, 1000, 40);
            TransactionReceipt receipt = processor.waitForTransactionReceipt(txHash);

            return receipt.getContractAddress();
        } finally {
            web3j.shutdown();
        }
    }
}
