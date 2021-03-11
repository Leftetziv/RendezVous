/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rendezvous.repository;

import com.rendezvous.entity.Client;
import com.rendezvous.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Leyteris
 */
@Repository
public interface ClientRepository extends JpaRepository<Client,Integer>{
    Optional<Client> findClientByUserIdEmail(String email);
}
