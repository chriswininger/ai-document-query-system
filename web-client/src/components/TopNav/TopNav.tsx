import {NavLink} from "react-router";
import "./TopNav.css";

export default function TopNav() {
  return <ul className="top-nav">
    <li>
      <NavLink
        to="/"
        className={({ isActive }) =>
          isActive ? "active" : ""
        }
      >
        Chat
      </NavLink>
    </li>

    <li>
      <NavLink
        to="/vector-search"
        className={({ isActive }) =>
          isActive ? "active" : ""
        }
      >
        Vector Search
      </NavLink>
    </li>
  </ul>
}
