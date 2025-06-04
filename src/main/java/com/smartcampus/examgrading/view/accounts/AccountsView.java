package com.smartcampus.examgrading.view.accounts;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.Component;

public class AccountsView extends AppLayout {
    private final SessionService sessionService;

    public AccountsView(SessionService sessionService) {
        this.sessionService = sessionService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Smart Campus System");
        logo.addClassNames("text-l", "m-m");

        Button logout = new Button("Log out", e -> {
            sessionService.logout();
            UI.getCurrent().navigate("login");
        });

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.add(
                createTab(VaadinIcon.HOME, "Dashboard", AccountsDashboardView.class),
                createTab(VaadinIcon.MONEY, "Pending Fees", PendingFeesView.class),
                createTab(VaadinIcon.USER, "Faculty Salaries", FacultySalaryView.class)
        );
        addToDrawer(tabs);
    }

    private Tab createTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> navigationTarget) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink(viewName, navigationTarget);
        link.add(icon);
        link.setTabIndex(-1);

        return new Tab(link);
    }
} 