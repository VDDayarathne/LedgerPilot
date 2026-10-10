/* oxlint-disable react/only-export-components, react/set-state-in-effect */
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setAccessToken, setSessionExpiredHandler } from '../api/client'

export type User = { id: string; email: string; displayName: string; emailVerified: boolean; activeTenantId?: string }
type AuthValue = { user: User | null; ready: boolean; expired: boolean; login(email:string,password:string):Promise<void>; logout():Promise<void>; refresh():Promise<void>; selectTenant(id:string):Promise<void>; updateProfile(displayName:string):Promise<void>; clearExpired():void }
const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user,setUser]=useState<User|null>(null), [ready,setReady]=useState(false), [expired,setExpired]=useState(false)
  const apply=(data:{accessToken:string;user:User})=>{setAccessToken(data.accessToken);setUser(data.user);setExpired(false)}
  const refresh=async()=>{const {data}=await api.post('/auth/refresh');apply(data)}
  useEffect(()=>{setSessionExpiredHandler(()=>{setUser(null);setExpired(true)});refresh().catch(()=>setAccessToken(null)).finally(()=>setReady(true))},[])
  const login=async(email:string,password:string)=>{const {data}=await api.post('/auth/login',{email,password});apply(data)}
  const logout=async()=>{try{await api.post('/auth/logout')}finally{setAccessToken(null);setUser(null)}}
  const selectTenant=async(id:string)=>{const {data}=await api.post(`/organizations/${id}/select`);apply(data)}
  const updateProfile=async(displayName:string)=>{const {data}=await api.put('/auth/me',{displayName});setUser(data)}
  const value=useMemo(()=>({user,ready,expired,login,logout,refresh,selectTenant,updateProfile,clearExpired:()=>setExpired(false)}),[user,ready,expired])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export const useAuth=()=>{const value=useContext(AuthContext);if(!value)throw new Error('AuthProvider missing');return value}
