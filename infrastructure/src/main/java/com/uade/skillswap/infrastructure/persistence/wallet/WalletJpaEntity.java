package com.uade.skillswap.infrastructure.persistence.wallet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallet")
public class WalletJpaEntity {

    @Id
    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(nullable = false)
    private long balance;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.PERSIST)
    @OrderBy("sequenceNumber")
    @BatchSize(size = 50)
    private List<WalletMovementJpaEntity> movements = new ArrayList<>();

    protected WalletJpaEntity() {
    }

    WalletJpaEntity(String ownerId, long balance) {
        this.ownerId = ownerId;
        this.balance = balance;
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
}
