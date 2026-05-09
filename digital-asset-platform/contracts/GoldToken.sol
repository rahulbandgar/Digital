// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";
import "@openzeppelin/contracts/access/AccessControl.sol";
import "@openzeppelin/contracts/utils/Pausable.sol";

/**
 * GoldToken (GLD) — ERC-20 representing 1 gram of physical gold per token.
 *
 * Roles:
 *   DEFAULT_ADMIN_ROLE  — grant/revoke roles
 *   MINTER_ROLE         — mint new tokens (admin backend only)
 *   BURNER_ROLE         — burn tokens on redemption
 *   PAUSER_ROLE         — emergency pause
 */
contract GoldToken is ERC20, ERC20Burnable, AccessControl, Pausable {

    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");

    uint8 private constant DECIMALS = 18;

    event TokensMinted(address indexed to, uint256 amount, string reason);
    event TokensBurned(address indexed from, uint256 amount, string reason);
    event Transferred(address indexed from, address indexed to, uint256 amount, string memo);

    constructor(address admin) ERC20("GoldToken", "GLD") {
        _grantRole(DEFAULT_ADMIN_ROLE, admin);
        _grantRole(MINTER_ROLE, admin);
        _grantRole(BURNER_ROLE, admin);
        _grantRole(PAUSER_ROLE, admin);
    }

    function decimals() public pure override returns (uint8) {
        return DECIMALS;
    }

    /**
     * Mint tokens when physical gold is deposited in the vault.
     * @param to       Recipient wallet address
     * @param amount   Token amount (in smallest unit, i.e. * 10^18)
     * @param reason   Audit reason (e.g. "Vault deposit: 10 kg gold")
     */
    function mint(address to, uint256 amount, string calldata reason)
        external
        onlyRole(MINTER_ROLE)
        whenNotPaused
    {
        require(to != address(0), "GoldToken: mint to zero address");
        require(amount > 0, "GoldToken: mint amount must be positive");
        _mint(to, amount);
        emit TokensMinted(to, amount, reason);
    }

    /**
     * Burn tokens when gold is redeemed from the vault.
     * @param from     Token holder
     * @param amount   Token amount to burn
     * @param reason   Audit reason
     */
    function burnFrom(address from, uint256 amount, string calldata reason)
        external
        onlyRole(BURNER_ROLE)
        whenNotPaused
    {
        require(amount > 0, "GoldToken: burn amount must be positive");
        _burn(from, amount);
        emit TokensBurned(from, amount, reason);
    }

    /**
     * Transfer tokens with optional memo for compliance logging.
     */
    function transferWithMemo(address to, uint256 amount, string calldata memo)
        external
        whenNotPaused
        returns (bool)
    {
        bool ok = transfer(to, amount);
        if (ok) emit Transferred(msg.sender, to, amount, memo);
        return ok;
    }

    function pause() external onlyRole(PAUSER_ROLE) {
        _pause();
    }

    function unpause() external onlyRole(PAUSER_ROLE) {
        _unpause();
    }

    function _update(address from, address to, uint256 value)
        internal
        override
        whenNotPaused
    {
        super._update(from, to, value);
    }
}
