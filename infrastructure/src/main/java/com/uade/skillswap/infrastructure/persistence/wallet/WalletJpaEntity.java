package com.uade.skillswap.infrastructure.persistence.wallet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallet")
public class WalletJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, unique = true, length = 100)
    private String ownerId;

    @Column(nullable = false)
    private long balance;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt, id")
    private List<WalletMovementJpaEntity> movements = new ArrayList<>();

    protected WalletJpaEntity() {
    }

    static WalletJpaEntity create(String ownerId) {
        WalletJpaEntity entity = new WalletJpaEntity();
        entity.ownerId = ownerId;
        return entity;
    }

    Long getId() {
        return id;
    }

    String getOwnerId() {
        return ownerId;
    }

    long getBalance() {
        return balance;
    }

    void setBalance(long balance) {
        this.balance = balance;
    }

    List<WalletMovementJpaEntity> getMovements() {
        return movements;
    }

    void addMovement(WalletMovementJpaEntity movement) {
        movements.add(movement);
        movement.setWallet(this);
    }
}
