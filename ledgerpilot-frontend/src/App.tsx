import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { useEffect, useState } from 'react'
import { api } from './api/client'
import { AcceptInvitation, ForgotPassword, Login, Members, OrganizationList, OrganizationSettings, Profile, Register, ResetPassword, VerifyEmail } from './pages/pages'
import './App.css'

function Protected() {
  const { user,ready,expired }=useAuth(), location=useLocation()
  if(!ready)return <div className="center-card"><span className="spinner"/> Restoring your session…</div>
  if(!user)return <Navigate to={expired?'/session-expired':'/login'} replace state={{from:location.pathname}} />
  return <Outlet/>
}
function TenantRequired(){const {user}=useAuth();return user?.activeTenantId?<Outlet/>:<Navigate to="/organizations" replace/>}
function Shell(){const {user,logout}=useAuth();const navigate=useNavigate(),[role,setRole]=useState('');useEffect(()=>{api.get('/organization').then(r=>setRole(r.data.role)).catch(()=>setRole(''))},[user?.activeTenantId]);const admin=role==='OWNER'||role==='ADMIN';return <div className="app-shell"><aside><div className="brand"><span>LP</span> LedgerPilot</div><nav><NavLink to="/app">Overview</NavLink>{admin&&<NavLink to="/app/organization">Organization</NavLink>}{admin&&<NavLink to="/app/members">People & roles</NavLink>}<NavLink to="/app/profile">Profile & security</NavLink><NavLink to="/organizations">Switch organization</NavLink></nav><button className="ghost" onClick={()=>logout().then(()=>navigate('/login'))}>Sign out</button></aside><main><header><div><small>ACTIVE WORKSPACE</small><strong>{user?.email}</strong></div></header><Outlet/></main></div>}
function Overview(){return <section className="page"><div className="eyebrow">FOUNDATION</div><h1>Your accounting workspace is ready.</h1><p className="lead">Authentication, membership access, and tenant boundaries are active. Financial modules arrive in later phases.</p><div className="stat-grid"><article><b>Secure sessions</b><span>Rotating refresh tokens and session controls</span></article><article><b>Tenant isolation</b><span>Every operation validates active membership</span></article><article><b>Auditable access</b><span>Security and administration events recorded</span></article></div></section>}
function SessionExpired(){const {clearExpired}=useAuth();return <div className="center-card"><div className="eyebrow">SESSION ENDED</div><h1>Please sign in again</h1><p>Your session expired or access changed.</p><NavLink className="button" to="/login" onClick={clearExpired}>Return to sign in</NavLink></div>}

export default function App(){return <BrowserRouter><AuthProvider><Routes><Route path="/login" element={<Login/>}/><Route path="/register" element={<Register/>}/><Route path="/verify-email" element={<VerifyEmail/>}/><Route path="/forgot-password" element={<ForgotPassword/>}/><Route path="/reset-password" element={<ResetPassword/>}/><Route path="/accept-invitation" element={<AcceptInvitation/>}/><Route path="/session-expired" element={<SessionExpired/>}/><Route element={<Protected/>}><Route path="/organizations" element={<OrganizationList/>}/><Route element={<TenantRequired/>}><Route path="/app" element={<Shell/>}><Route index element={<Overview/>}/><Route path="organization" element={<OrganizationSettings/>}/><Route path="members" element={<Members/>}/><Route path="profile" element={<Profile/>}/></Route></Route></Route><Route path="/access-denied" element={<div className="center-card"><h1>Access denied</h1><p>You do not have permission for this action.</p></div>}/><Route path="*" element={<Navigate to="/app" replace/>}/></Routes></AuthProvider></BrowserRouter>}
