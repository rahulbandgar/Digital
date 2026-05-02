package com.digital.erc20.contract;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

public class ERC20Deployer {

    private static final long POLL_INTERVAL_MS = 3000;
    private static final int MAX_ATTEMPTS = 60;

    private final Web3j web3j;
    private final Credentials credentials;
    private final long chainId;

    public ERC20Deployer(String rpcUrl, String privateKey, long chainId) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);
        this.chainId = chainId;
    }

    public String getAddress() {
        return credentials.getAddress();
    }

    public BigInteger getBalance() throws Exception {
        return web3j.ethGetBalance(credentials.getAddress(),
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                .send().getBalance();
    }

    public DeployResult deploy(String tokenName, String tokenSymbol, BigInteger initialSupply,
                               Consumer<String> statusCallback) throws Exception {

        String bytecode = loadBytecode();
        String encodedConstructor = encodeConstructor(tokenName, tokenSymbol, initialSupply);
        String deployData = bytecode + encodedConstructor;

        statusCallback.accept("Estimating gas...");
        BigInteger gasLimit = estimateGas(deployData);
        statusCallback.accept("Gas estimated: " + gasLimit);

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        statusCallback.accept("Gas price: " + gasPrice + " wei");

        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, chainId);
        StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);

        statusCallback.accept("Sending deployment transaction...");

        EthSendTransaction txResponse = txManager.sendTransaction(
                gasPrice, gasLimit, null, deployData, BigInteger.ZERO);

        if (txResponse.hasError()) {
            throw new RuntimeException("Transaction failed: " + txResponse.getError().getMessage());
        }

        String txHash = txResponse.getTransactionHash();
        statusCallback.accept("Transaction sent! Hash: " + txHash);
        statusCallback.accept("Waiting for confirmation...");

        TransactionReceipt receipt = waitForReceipt(txHash, statusCallback);

        String contractAddress = receipt.getContractAddress();
        return new DeployResult(txHash, contractAddress, receipt.getBlockNumber().toString());
    }

    private String loadBytecode() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/contracts/SimpleERC20.bin")) {
            if (is == null) throw new RuntimeException("Bytecode resource not found");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private String encodeConstructor(String name, String symbol, BigInteger supply) {
        Function constructor = new Function("",
                Arrays.asList(new Utf8String(name), new Utf8String(symbol), new Uint256(supply)),
                Collections.emptyList());
        return FunctionEncoder.encode(constructor).substring(10); // strip 4-byte selector
    }

    private BigInteger estimateGas(String data) {
        try {
            org.web3j.protocol.core.methods.request.Transaction tx =
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            credentials.getAddress(), null, data);
            BigInteger estimated = web3j.ethEstimateGas(tx).send().getAmountUsed();
            // Add 20% buffer
            return estimated.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
        } catch (Exception e) {
            return DefaultGasProvider.GAS_LIMIT;
        }
    }

    private TransactionReceipt waitForReceipt(String txHash, Consumer<String> statusCallback) throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(txHash).send();
            Optional<TransactionReceipt> receipt = response.getTransactionReceipt();
            if (receipt.isPresent()) {
                return receipt.get();
            }
            statusCallback.accept("Waiting for block confirmation... (" + (i + 1) + "/" + MAX_ATTEMPTS + ")");
        }
        throw new RuntimeException("Transaction not mined after " + MAX_ATTEMPTS + " attempts");
    }

    public void shutdown() {
        web3j.shutdown();
    }

    public record DeployResult(String txHash, String contractAddress, String blockNumber) {}
}
