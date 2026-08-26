import { useState } from 'react';
import { Layout, type Vista } from './components/Layout';
import { Dashboard } from './modules/dashboard/Dashboard';
import { Clientes } from './modules/clientes/Clientes';
import { Toldos } from './modules/toldos/Toldos';
import { Alquileres } from './modules/alquileres/Alquileres';
import { Recibos } from './modules/recibos/Recibos';
import { Configuracion } from './modules/configuracion/Configuracion';

export default function App() {
  const [vista, setVista] = useState<Vista>('panel');
  const navegar = (v: Vista) => setVista(v);

  return (
    <Layout vista={vista} alCambiarVista={navegar}>
      {vista === 'panel' && <Dashboard navegar={navegar} />}
      {vista === 'clientes' && <Clientes />}
      {vista === 'toldos' && <Toldos />}
      {vista === 'alquileres' && <Alquileres navegar={navegar} />}
      {vista === 'recibos' && <Recibos />}
      {vista === 'configuracion' && <Configuracion />}
    </Layout>
  );
}
