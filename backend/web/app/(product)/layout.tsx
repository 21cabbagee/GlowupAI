import { AppShell } from "@/components/shell";

export default function ProductLayout({ children }: LayoutProps<"/">) {
  return <AppShell>{children}</AppShell>;
}
