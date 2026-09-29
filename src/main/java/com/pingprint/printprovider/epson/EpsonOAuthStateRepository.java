package com.pingprint.printprovider.epson;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EpsonOAuthStateRepository extends JpaRepository<EpsonOAuthState, String> { }
