import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  isAuthPage = true;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.checkRoute(this.router.url);
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(e => this.checkRoute(e.urlAfterRedirects));
  }

  private checkRoute(url: string): void {
    this.isAuthPage = url === '/' || url.startsWith('/register');
  }
}
