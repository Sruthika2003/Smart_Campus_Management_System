package com.smartcampus.examgrading.view.admin;

import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin Dashboard | Smart Campus System")
public class AdminView extends VerticalLayout implements RouterLayout {

    public AdminView() {
        add(
                new H2("Admin Dashboard"),
                new Paragraph(
                        "Welcome to the Admin Dashboard. Use the navigation menu to manage courses, users, and system settings."));

        setSizeFull();
        setSpacing(true);
        setPadding(true);
    }
}