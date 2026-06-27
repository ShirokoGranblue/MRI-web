import { HashRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppProvider } from './lib/app-context.jsx';
import { RequireAuth, default as AppLayout } from './components/AppLayout.jsx';
import ActivityPage from './pages/ActivityPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import ExamsPage from './pages/ExamsPage.jsx';
import ImagesPage from './pages/ImagesPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import PatientsPage from './pages/PatientsPage.jsx';
import ReportsPage from './pages/ReportsPage.jsx';
import SettingsPage from './pages/SettingsPage.jsx';
import {
  PatientDashboardPage,
  PatientExamsPage,
  PatientImagesPage,
  PatientProfilePage,
  PatientReportsPage,
} from './pages/PatientPortalPages.jsx';
import { useApp } from './lib/app-context.jsx';

export default function App() {
  return (
    <AppProvider>
      <HashRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <RequireAuth>
                <AppLayout />
              </RequireAuth>
            }
          >
            <Route path="/" element={<RolePage doctor={DashboardPage} patient={PatientDashboardPage} />} />
            <Route path="/patients" element={<RolePage doctor={PatientsPage} patient={PatientProfilePage} />} />
            <Route path="/exams" element={<RolePage doctor={ExamsPage} patient={PatientExamsPage} />} />
            <Route path="/images" element={<RolePage doctor={ImagesPage} patient={PatientImagesPage} />} />
            <Route path="/reports" element={<RolePage doctor={ReportsPage} patient={PatientReportsPage} />} />
            <Route path="/settings" element={<DoctorOnly><SettingsPage /></DoctorOnly>} />
            <Route path="/activity" element={<DoctorOnly><ActivityPage /></DoctorOnly>} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </HashRouter>
    </AppProvider>
  );
}

function RolePage({ doctor: DoctorPage, patient: PatientPage }) {
  const { isPatient } = useApp();
  return isPatient ? <PatientPage /> : <DoctorPage />;
}

function DoctorOnly({ children }) {
  const { isPatient } = useApp();
  return isPatient ? <Navigate to="/" replace /> : children;
}
