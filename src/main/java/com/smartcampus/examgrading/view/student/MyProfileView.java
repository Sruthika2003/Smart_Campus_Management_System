package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.smartcampus.examgrading.view.MainLayout;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("My Profile | Smart Campus")
public class MyProfileView extends VerticalLayout {

    public MyProfileView(SecurityService securityService) {
        User currentUser = securityService.getCurrentUser();
        
        add(new H2("My Profile"));
        
        if (currentUser != null) {
            add(
                new Span("Name: " + currentUser.getFirstName() + " " + currentUser.getLastName()),
                new Span("Email: " + currentUser.getEmail()),
                new Span("Role: " + currentUser.getRole())
            );
        } else {
            add(new Span("Not logged in"));
        }
        
        setSpacing(true);
        setPadding(true);
    }
} 