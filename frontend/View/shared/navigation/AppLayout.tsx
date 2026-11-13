import { Outlet } from "react-router-dom";
import { Navbar } from "./Navbar";
import "./AppLayout.css";

/**
 * Layout für alle Seiten nach dem Login.
 * Desktop: Navigation als Sidebar links. Mobile/Tablet: Navigation unten.
 */
export const AppLayout = () => {
  return (
    <div className="shell">
      <main className="shell-content">
        <Outlet />
      </main>
      <Navbar />
    </div>
  );
};
