import { NavLink } from "react-router-dom";
import {
  CircleUserRound,
  ContactRound,
  FileText,
  House,
  LogOut,
  Plus,
} from "lucide-react";
import { Link } from "react-router-dom";
import wordmark from "../../../shared/assets/schriftzug-weiss.svg";
import "./navbar-css.css";

const navItems = [
  { to: "/dashboard", label: "Start", icon: House, end: true },
  { to: "/dokumente", label: "Dokumente", icon: FileText, end: false },
  { to: "/kunden", label: "Kunden", icon: ContactRound, end: false },
  { to: "/profil", label: "Profil", icon: CircleUserRound, end: false },
];

export const Navbar = () => {
  return (
    <nav className="navbar wm-navbar">
      <span className="navbar-logo-wrapper">
        <img className="navbar-logo" src={wordmark} alt="Werkmundo" />
      </span>

      <Link className="wm-create-link" to="/home" aria-label="Direkt ein Angebot erstellen">
        <Plus size={17} strokeWidth={2.5} aria-hidden="true" />
        <span>Angebot erstellen</span>
      </Link>

      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          className={({ isActive }) =>
            isActive ? "nav-item active" : "nav-item"
          }
        >
          <span className="nav-icon">
            <item.icon size={22} strokeWidth={2} aria-hidden="true" />
          </span>
          <span className="nav-label">{item.label}</span>
        </NavLink>
      ))}

      <NavLink to="/abmelden" className="wm-logout-link">
        <LogOut size={18} aria-hidden="true" />
        <span>Abmelden</span>
      </NavLink>
    </nav>
  );
};
