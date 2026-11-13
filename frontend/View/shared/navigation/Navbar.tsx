import { NavLink } from "react-router-dom";
import {
  CircleUserRound,
  ContactRound,
  FileText,
  LayoutDashboard,
} from "lucide-react";
import "./navbar-css.css";

const navItems = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/rechnungen", label: "Rechnungen", icon: FileText },
  { to: "/kunden", label: "Kunden", icon: ContactRound },
  { to: "/profil", label: "Profil", icon: CircleUserRound },
];

export const Navbar = () => {
  return (
    <nav className="navbar wm-navbar" aria-label="Hauptnavigation">
      {/* Wortmarke – nur auf Desktop sichtbar (CSS steuert das) */}
      <span className="navbar-logo-wrapper">
        <span className="wm-navbar-wordmark">
          Werk<span>mundo</span>
        </span>
      </span>

      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) => (isActive ? "nav-item active" : "nav-item")}
        >
          <span className="nav-icon">
            <item.icon size={22} strokeWidth={2} aria-hidden="true" />
          </span>
          <span className="nav-label">{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
};
