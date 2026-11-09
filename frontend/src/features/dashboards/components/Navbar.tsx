import { NavLink } from "react-router-dom";
import {
  ContactRound,
  CircleUserRound,
  FileText,
  House,
  LogOut,
  Plus,
} from "lucide-react";
import werkmundoWordmark from "@/features/landing/assets/werkmundo-wordmark.png";
import "./navbar-css.css";

const navItems = [
  { to: "/dashboard", label: "Start", icon: House },
  { to: "/dokumente", label: "Dokumente", icon: FileText },
  { to: "/unternehmen?tab=kunde", label: "Kunden", icon: ContactRound },
  { to: "/profil", label: "Profil", icon: CircleUserRound },
];

export const Navbar = () => {
  return (
    <nav className="navbar wm-navbar">
      {/* Logo — nur auf Desktop sichtbar (CSS steuert das) */}
      <span className="navbar-logo-wrapper">
        <img src={werkmundoWordmark} alt="Werkmundo" className="navbar-logo" />
      </span>

      <NavLink
        to="/home"
        className={({ isActive }) =>
          isActive ? "wm-create-link active" : "wm-create-link"
        }
      >
        <Plus size={18} aria-hidden="true" />
        <span>Neues Angebot</span>
      </NavLink>

      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === "/"}
          className={({ isActive }) =>
            isActive ? "nav-item active" : "nav-item"
          }
        >
          <span className="nav-icon">
            <item.icon size={22} strokeWidth={2} />
          </span>
          <span className="nav-label">{item.label}</span>
        </NavLink>
      ))}

      <NavLink to="/logout" className="wm-logout-link">
        <LogOut size={18} aria-hidden="true" />
        <span>Abmelden</span>
      </NavLink>
    </nav>
  );
};
