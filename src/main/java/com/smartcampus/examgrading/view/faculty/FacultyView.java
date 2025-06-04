package com.smartcampus.examgrading.view.faculty;

import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

@Route(value = "faculty", layout = MainLayout.class)
@PageTitle("Faculty Dashboard | Course Management System")
public class FacultyView extends VerticalLayout implements RouterLayout {

    public FacultyView() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        add(new H2("Faculty Dashboard"));
        add(new Paragraph("Welcome to the Faculty Dashboard. Use the navigation menu to manage your courses and view enrolled students."));
    }
}