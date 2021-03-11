/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rendezvous.controller;

import com.rendezvous.entity.Client;
import com.rendezvous.service.ClientService;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/client")
public class ClientController {

    @Autowired
    ClientService clientService;

    @GetMapping("/dashboard")
    public String showDashboard() {
        return "client/dashboard_client";
    }

    @GetMapping("/profile")
    public String showProfile(@ModelAttribute("client") Client client) {

        //todo
        //client = //o logarismenos client. Tha ton xrisomopoiei i forma diorthoseis stoixeion tou xristi sto profile_client
        return "client/profile_client";
    }
//    @PostMapping("/profile")
//    public String updateProfile(@ModelAttribute("client") Client client ) {
//        
//        //todo
//        //ananeosi tou client stin vasi
//        return "redirect:/client/dashboard";
//    }

    @GetMapping("/comp-select")
    public String showCompanySelect() {
        return "client/company_search";
    }

    @PostMapping("/comp-select")
    public String showCompanySelect(@RequestParam int companyId, Model model) {
        model.addAttribute("comp_id", companyId); //comp_id will be used by company_date_pick
        return "client/company_date_pick";
    }

    @ModelAttribute
    public void addAttributes(Principal principal, Model model) {

        if (principal != null) {
            Client c = clientService.findClientByEmail(principal.getName());
            model.addAttribute("username", c.getFname() + " " + c.getLname());
        }
    }
}
